from google.adk.agents import Agent
from google.adk.tools import AgentTool
from google.adk.agents.callback_context import CallbackContext
from PIL import Image, ImageEnhance
from .rag import retrieve
import base64
import io

_state = {"image_bytes": None, "mime_type": "image/jpeg"}

def save_incoming_image(callback_context: CallbackContext):
    try:
        user_content = callback_context._invocation_context.user_content
        if not user_content:
            return None
        for part in user_content.parts:
            if hasattr(part, "inline_data") and part.inline_data:
                mime_type = part.inline_data.mime_type or "image/jpeg"
                data = part.inline_data.data
                image_bytes = bytes(data) if isinstance(data, (bytes, bytearray)) else base64.b64decode(data)
                _state["image_bytes"] = image_bytes
                _state["mime_type"] = mime_type
                break
    except Exception as e:
        print(f"[callback] Error: {e}")
    return None

def edit_image(
    brightness: float = 1.0,
    contrast: float = 1.0,
    saturation: float = 1.0,
) -> str:
    if _state["image_bytes"] is None:
        return "ERROR: No image in memory."

    img = Image.open(io.BytesIO(_state["image_bytes"]))
    img = ImageEnhance.Brightness(img).enhance(brightness)
    img = ImageEnhance.Contrast(img).enhance(contrast)
    img = ImageEnhance.Color(img).enhance(saturation)

    output_buffer = io.BytesIO()
    fmt = {"image/jpeg": "JPEG", "image/png": "PNG",
           "image/gif": "GIF", "image/webp": "WEBP"}.get(_state["mime_type"], "JPEG")
    img.save(output_buffer, format=fmt)
    _state["image_bytes"] = output_buffer.getvalue()

    return (
        f"Image edited — "
        f"brightness={brightness}, contrast={contrast}, saturation={saturation}."
    )

# ── Sub-agents: kept intentionally concise to minimise token round-trips ──────

agent_protanopia = Agent(
    model="gemini-2.5-flash",
    name="agent_protanopia",
    instruction="""
    You are a protanopia (red-blindness) expert. Missing L-cones. ~2% of males.
    Key fact: reds appear near-BLACK (luminance also lost), unlike deuteranopia.

    STEPS (do all in ONE reply, no follow-up turns):
    1. Call retrieve once with a tight query, e.g. "protanopia L-cone red perception".
    2. For each colour region in the description:
       - actual colour → perceived colour → indistinguishable pairs if any.
    3. One closing sentence on the near-black red effect.
    Be concise. No preamble.
    """,
    tools=[retrieve],
)

agent_deuteranopia = Agent(
    model="gemini-2.5-flash",
    name="agent_deuteranopia",
    instruction="""
    You are a deuteranopia (green-blindness) expert. Missing M-cones. ~6% of males.
    Key fact: reds appear dark olive/brown (NOT near-black) — L-cone luminance intact.

    STEPS (do all in ONE reply, no follow-up turns):
    1. Call retrieve once, e.g. "deuteranopia M-cone green red perception".
    2. For each colour region in the description:
       - actual colour → perceived colour → indistinguishable pairs if any.
    3. One closing sentence on olive/brown reds.
    Be concise. No preamble.
    """,
    tools=[retrieve],
)

agent_blue_yellow = Agent(
    model="gemini-2.5-flash",
    name="agent_blue_yellow",
    instruction="""
    You are a tritanopia (blue-yellow blindness) expert. Missing S-cones. Very rare.
    Often acquired (ageing, glaucoma, diabetes).

    STEPS (do all in ONE reply, no follow-up turns):
    1. Call retrieve once, e.g. "tritanopia S-cone blue yellow perception".
    2. For each colour region in the description:
       - actual colour → perceived colour → indistinguishable pairs if any.
    Be concise. No preamble.
    """,
    tools=[retrieve],
)

agent_complete_blindness = Agent(
    model="gemini-2.5-flash",
    name="agent_complete_blindness",
    instruction="""
    You are an achromatopsia (complete colour blindness) expert. No cone cells.
    Everything is shades of grey. ~1 in 30,000 people.

    STEPS (do all in ONE reply, no follow-up turns):
    1. Call retrieve once, e.g. "achromatopsia luminance greyscale rod vision".
    2. For each colour region: actual colour → grey shade (use L=0.299R+0.587G+0.114B)
       → flag same-luminance pairs.
    3. Briefly note photophobia and ~20/200 acuity.
    Be concise. No preamble.
    """,
    tools=[retrieve],
)

# ── Root agent ────────────────────────────────────────────────────────────────

root_agent = Agent(
    model="gemini-2.5-flash",
    name="root_agent",
    before_agent_callback=save_incoming_image,
    instruction="""
    You are the coordinator for a colour-blindness assistant app.
    You are the ONLY agent that can see the image.

    The user's message will always start with:
      "The user has <TYPE>. Please analyse this image for someone with <TYPE>.
       Skip asking which type of colour blindness they have — it is already known."

    WORKFLOW — complete in ONE turn, no clarifying questions:

    1. Read the colour-blindness type from the user's message.
       If it is missing or unclear, default to deuteranopia.

    2. Analyse the image immediately:
       - List every distinct object/region with its exact colour in 1–2 sentences
         (e.g. "red apple, green leaf, white plate, wooden brown table").
       - Only call edit_image if the image is clearly too dark (brightness < 0.4)
         or too washed-out. Skip it otherwise to save time.

    3. Pass ONLY the colour description (plain text, no image) to the correct agent:
         protanopia      → agent_protanopia
         deuteranopia    → agent_deuteranopia
         tritanopia / blue-yellow → agent_blue_yellow
         achromatopsia / complete → agent_complete_blindness

    4. Return the sub-agent's reply VERBATIM. Do not rephrase, summarize, or add anything.
    """,
    tools=[
        edit_image,
        AgentTool(agent=agent_protanopia),
        AgentTool(agent=agent_deuteranopia),
        AgentTool(agent=agent_blue_yellow),
        AgentTool(agent=agent_complete_blindness),
    ],
)
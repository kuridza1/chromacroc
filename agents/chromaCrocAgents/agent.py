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
                print("[callback] Image stored in memory.")
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
        f"Image edited in memory — "
        f"brightness={brightness}, contrast={contrast}, saturation={saturation}."
    )

agent_protanopia = Agent(
    model="gemini-2.5-flash",
    name="agent_protanopia",
    instruction="""
    You are a protanopia (red-blindness) assistant.
    Protanopia is caused by missing L-cones (long-wavelength / red-sensitive).
    Prevalence: ~2% of males, ~0.04% of females.

    You have access to a scientific knowledge base via the retrieve tool.
    Use it to look up accurate perceptual information before answering.

    WORKFLOW:
    1. Call retrieve with a query relevant to what you need to explain,
       e.g. "protanopia red colour perception L-cone" or
            "confusion lines LMS protanopia red green".
    2. Use the retrieved passages to ground your explanation scientifically.
    3. For each object or colour region in the description you receive:
         a. State the actual colour.
         b. Explain what a protanope would perceive, backed by retrieved knowledge.
         c. Flag any pairs that would be indistinguishable.
    4. Highlight the key difference from deuteranopia: in protanopia, reds
       appear near-BLACK because the L-cone's luminance contribution is also lost.

    Be specific and empathetic. Cite what you retrieved where relevant.
    """,
    tools=[retrieve],
)

agent_deuteranopia = Agent(
    model="gemini-2.5-flash",
    name="agent_deuteranopia",
    instruction="""
    You are a deuteranopia (green-blindness) assistant.
    Deuteranopia is caused by missing M-cones (medium-wavelength / green-sensitive).
    Prevalence: ~6% of males, ~0.39% of females. The most common form of CVD.

    You have access to a scientific knowledge base via the retrieve tool.
    Use it to look up accurate perceptual information before answering.

    WORKFLOW:
    1. Call retrieve with a query relevant to what you need to explain,
       e.g. "deuteranopia green colour perception M-cone" or
            "deuteranopia vs protanopia red brightness luminance".
    2. Use the retrieved passages to ground your explanation scientifically.
    3. For each object or colour region in the description you receive:
         a. State the actual colour.
         b. Explain what a deuteranope would perceive, backed by retrieved knowledge.
         c. Flag any pairs that would be indistinguishable.
    4. Highlight the key difference from protanopia: in deuteranopia, reds appear
       as dark olive/brown (NOT near-black) because the L-cone luminance is intact.

    Be specific and empathetic. Cite what you retrieved where relevant.
    """,
    tools=[retrieve],
)

agent_blue_yellow = Agent(
    model="gemini-2.5-flash",
    name="agent_blue_yellow",
    instruction="""
    You are a tritanopia (blue-yellow blindness) assistant.
    Tritanopia is caused by missing S-cones (short-wavelength / blue-sensitive).
    Prevalence: ~0.002% of males. Affects all genders equally. Often acquired,
    not inherited (causes: ageing, glaucoma, diabetes).

    You have access to a scientific knowledge base via the retrieve tool.
    Use it to look up accurate perceptual information before answering.

    WORKFLOW:
    1. Call retrieve with a query relevant to what you need to explain,
       e.g. "tritanopia blue yellow colour perception S-cone" or
            "tritanopia confusion lines blue green yellow pink".
    2. Use the retrieved passages to ground your explanation scientifically.
    3. For each object or colour region in the description you receive:
         a. State the actual colour.
         b. Explain what a tritanope would perceive, backed by retrieved knowledge.
         c. Flag any pairs that would be indistinguishable.

    Be specific and empathetic. Cite what you retrieved where relevant.
    """,
    tools=[retrieve],
)

agent_complete_blindness = Agent(
    model="gemini-2.5-flash",
    name="agent_complete_blindness",
    instruction="""
    You are an achromatopsia (complete colour blindness) assistant.
    Achromatopsia means no functioning cone cells — only rod cells remain.
    Prevalence: ~1 in 30,000 people. Everything is perceived as shades of grey.

    You have access to a scientific knowledge base via the retrieve tool.
    Use it to look up accurate perceptual information before answering.

    WORKFLOW:
    1. Call retrieve with a query relevant to what you need to explain,
       e.g. "achromatopsia monochromacy luminance grey perception" or
            "ITU luminance formula RGB greyscale colour blindness".
    2. Use the retrieved passages to ground your explanation scientifically.
    3. For each object or colour region in the description you receive:
         a. State the actual colour.
         b. Estimate its luminance (L = 0.299R + 0.587G + 0.114B) and describe
            the grey shade perceived.
         c. Flag colour pairs that become indistinguishable due to similar luminance.
    4. Mention relevant additional symptoms where appropriate: photophobia,
       poor visual acuity (~20/200), possible nystagmus.

    Be specific and empathetic. Cite what you retrieved where relevant.
    """,
    tools=[retrieve],
)

root_agent = Agent(
    model="gemini-2.5-flash",
    name="root_agent",
    before_agent_callback=save_incoming_image,
    instruction="""
    You are the main coordinator for a colour blindness assistant app.
    You are the ONLY agent that can see the image — all sub-agents receive
    text only, so you must do all visual analysis yourself.

    WORKFLOW:
    1. When an image arrives, ask the user which type of colour blindness
       they have (or are simulating):
         a) Protanopia       — red blindness   (~2% of males)
         b) Deuteranopia     — green blindness (~6% of males, most common)
         c) Blue-yellow      — tritanopia      (very rare, all genders)
         d) Complete         — achromatopsia   (~1 in 30,000)

    2. Once they answer, analyse the image yourself:
         - List every distinct object/region with its exact colour
           (e.g. "deep crimson red apple", "muted olive green leaf").
         - Decide edit values to improve colour clarity:
             Too dark      → brightness 1.3–1.6
             Too flat      → contrast   1.2–1.5
             Dull colours  → saturation 1.2–1.4
             Already good  → all 1.0
             Too bright    → brightness 0.6–0.9
         - Call edit_image with those values.

    3. Pass your colour description as plain text to the correct agent:
         - Protanopia   → agent_protanopia
         - Deuteranopia → agent_deuteranopia
         - Blue-yellow  → agent_blue_yellow
         - Complete     → agent_complete_blindness

    4. Return their full explanation to the user.
    """,
    tools=[
        edit_image,
        AgentTool(agent=agent_protanopia),
        AgentTool(agent=agent_deuteranopia),
        AgentTool(agent=agent_blue_yellow),
        AgentTool(agent=agent_complete_blindness),
    ],
)

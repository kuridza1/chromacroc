from google.adk.agents import Agent
from google.adk.tools import AgentTool
from google.adk.agents.callback_context import CallbackContext
from PIL import Image, ImageEnhance
import base64
import os
import uuid

_state = {"image_bytes": None, "mime_type": "image/jpeg", "image_path": None}

SAVE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "edited_images")
os.makedirs(SAVE_DIR, exist_ok=True)


# ── Callback ───────────────────────────────────────────────────────────────────

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

                ext = {"image/jpeg": ".jpg", "image/png": ".png",
                       "image/gif": ".gif", "image/webp": ".webp"}.get(mime_type, ".jpg")
                saved_path = os.path.join(SAVE_DIR, f"{uuid.uuid4().hex}{ext}")
                with open(saved_path, "wb") as f:
                    f.write(image_bytes)

                _state["image_bytes"] = image_bytes
                _state["mime_type"] = mime_type
                _state["image_path"] = saved_path
                print(f"[callback] Image saved to: {saved_path}")
                break
    except Exception as e:
        print(f"[callback] Error: {e}")
    return None


# ── Edit tool ──────────────────────────────────────────────────────────────────

def edit_image(
    brightness: float = 1.0,
    contrast: float = 1.0,
    saturation: float = 1.0,
) -> str:
    """
    Edit the current image to improve colour clarity.

    Args:
        brightness: >1 brightens, <1 darkens (default 1.0).
        contrast:   >1 more contrast          (default 1.0).
        saturation: >1 more vivid             (default 1.0).

    Returns:
        Path to the saved edited image.
    """
    image_path = _state["image_path"]
    if not image_path or not os.path.exists(image_path):
        return f"ERROR: No image loaded. _state={_state}"
    img = Image.open(image_path)
    img = ImageEnhance.Brightness(img).enhance(brightness)
    img = ImageEnhance.Contrast(img).enhance(contrast)
    img = ImageEnhance.Color(img).enhance(saturation)
    base, ext = os.path.splitext(image_path)
    output_path = f"{base}_edited{ext}"
    img.save(output_path)
    return f"Image saved to: {output_path}"


# ── Sub-agenti ─────────────────────────────────────────────────────────────────

agent_protanopia = Agent(
    model="gemini-2.5-flash",
    name="agent_protanopia",
    instruction="Protanopia expert. Reds appear near-black (L-cone absent). "
                "Answer the user's question in ONE sentence, max 15 words. No preamble.",
)

agent_deuteranopia = Agent(
    model="gemini-2.5-flash",
    name="agent_deuteranopia",
    instruction="Deuteranopia expert. Reds appear olive/brown (M-cone absent). "
                "Answer the user's question in ONE sentence, max 15 words. No preamble.",
)

agent_blue_yellow = Agent(
    model="gemini-2.5-flash",
    name="agent_blue_yellow",
    instruction="Tritanopia expert. Blues/yellows confused (S-cone absent). "
                "Answer the user's question in ONE sentence, max 15 words. No preamble.",
)

agent_complete_blindness = Agent(
    model="gemini-2.5-flash",
    name="agent_complete_blindness",
    instruction="Achromatopsia expert. Everything is grey (no cone cells). "
                "Answer the user's question in ONE sentence, max 15 words. No preamble.",
)


# ── Root agent ─────────────────────────────────────────────────────────────────

root_agent = Agent(
    model="gemini-2.5-flash",
    name="root_agent",
    before_agent_callback=save_incoming_image,
    instruction="""
Colour-blindness assistant. You see the image; sub-agents do not.
Message starts with "The user has <TYPE>." Default: deuteranopia.

1. Optionally call edit_image if image is too dark/flat/dull:
     Too dark      → brightness 1.3–1.6
     Too flat      → contrast   1.2–1.5
     Dull colours  → saturation 1.2–1.4
     Already good  → skip
     Too bright    → brightness 0.6–0.9

2. Describe image colours in one short sentence.

3. Send that sentence + user's question to the correct sub-agent:
   protanopia             → agent_protanopia
   deuteranopia           → agent_deuteranopia
   tritanopia/blue-yellow → agent_blue_yellow
   achromatopsia/complete → agent_complete_blindness

4. Return the sub-agent reply VERBATIM. Nothing else.
""",
    tools=[
        edit_image,
        AgentTool(agent=agent_protanopia),
        AgentTool(agent=agent_deuteranopia),
        AgentTool(agent=agent_blue_yellow),
        AgentTool(agent=agent_complete_blindness),
    ],
)
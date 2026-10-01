import { GoogleGenAI, Type, Schema } from "@google/genai";
import { OptionItem } from "../types";

// Helper to get the AI instance safely
const getAI = () => {
  // The API key must be obtained exclusively from the environment variable process.env.API_KEY.
  // Assume this variable is pre-configured, valid, and accessible.
  return new GoogleGenAI({ apiKey: process.env.API_KEY });
};

const modelFlash = 'gemini-3-flash-preview'; 
const fallbackDesignOptions: OptionItem[] = [
  { id: '1', title: 'Neon Arcade', description: 'Bright glowing lights and dark backgrounds.', tip: 'High contrast makes things easy to see!' },
  { id: '2', title: 'Paper Sketch', description: 'Looks like it was drawn in a notebook.', tip: 'Hand-drawn styles feel friendly and personal.' },
  { id: '3', title: 'Future Glass', description: 'Shiny, transparent, and super clean.', tip: 'Minimalism helps users focus on the content.' },
];
const fallbackLogicOptions: OptionItem[] = [
  { id: '1', title: 'Tap Master', description: 'Tap fast to win!', tip: 'Event Listeners wait for your clicks.' },
  { id: '2', title: 'Drag & Drop', description: 'Move items around the screen.', tip: 'Coordinates tell the computer where things are.' },
  { id: '3', title: 'Type It', description: 'Use the keyboard to control things.', tip: 'Input fields collect text from the user.' },
];

const escapeHtml = (text: string): string =>
  text.replace(/[&<>"']/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  })[character] || character);

const createOfflineGame = (idea: string, design: OptionItem, logic: OptionItem): string => {
  const palettes = design.title === 'Paper Sketch'
    ? { background: '#fff7e6', foreground: '#3f3024', accent: '#e56b45', card: '#fffdf7' }
    : design.title === 'Future Glass'
      ? { background: '#101b2d', foreground: '#f1f7ff', accent: '#42d9c8', card: '#1b2a40' }
      : { background: '#100d24', foreground: '#ffffff', accent: '#b45cff', card: '#21183c' };

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(idea || 'Your Game')}</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; min-height: 100vh; display: grid; place-items: center; padding: 24px; background: ${palettes.background}; color: ${palettes.foreground}; font: 18px system-ui, sans-serif; text-align: center; }
    main { width: min(100%, 520px); padding: 32px 24px; border-radius: 28px; background: ${palettes.card}; box-shadow: 0 18px 60px #0004; }
    h1 { margin: 8px 0; font-size: clamp(2rem, 8vw, 3.5rem); overflow-wrap: anywhere; }
    p { line-height: 1.5; opacity: .85; }
    .stats { display: flex; justify-content: center; gap: 32px; margin: 24px 0; font-weight: 700; }
    .stat-value { display: block; font-size: 2rem; color: ${palettes.accent}; }
    button { width: 150px; height: 150px; border: 0; border-radius: 50%; background: ${palettes.accent}; color: white; font-size: 4rem; cursor: pointer; box-shadow: 0 10px 0 #0003; touch-action: manipulation; }
    button:active { transform: translateY(5px); box-shadow: 0 5px 0 #0003; }
    button:disabled { cursor: default; opacity: .55; }
    .hint { font-size: .9rem; }
  </style>
</head>
<body>
  <main>
    <p>YOUR GAME</p>
    <h1>${escapeHtml(idea || 'Tap the Star!')}</h1>
    <p>${escapeHtml(logic.description)} Tap the star as many times as you can before time runs out!</p>
    <div class="stats">
      <div>TIME<span class="stat-value" id="time">20</span></div>
      <div>POINTS<span class="stat-value" id="score">0</span></div>
    </div>
    <button id="target" aria-label="Tap the star">⭐</button>
    <p class="hint" id="message">Ready? Go!</p>
  </main>
  <script>
    let time = 20;
    let score = 0;
    const timer = document.getElementById('time');
    const points = document.getElementById('score');
    const target = document.getElementById('target');
    const message = document.getElementById('message');
    target.addEventListener('click', () => {
      if (time > 0) points.textContent = String(++score);
    });
    const countdown = setInterval(() => {
      timer.textContent = String(--time);
      if (time <= 0) {
        clearInterval(countdown);
        target.disabled = true;
        message.textContent = 'Time is up! You scored ' + score + ' points. Build another app to play again!';
      }
    }, 1000);
  </script>
</body>
</html>`;
};

export const generateDesignOptions = async (idea: string): Promise<OptionItem[]> => {
  const schema: Schema = {
    type: Type.ARRAY,
    items: {
      type: Type.OBJECT,
      properties: {
        id: { type: Type.STRING },
        title: { type: Type.STRING },
        description: { type: Type.STRING },
        tip: { type: Type.STRING, description: "A short educational fact about UI/UX design suitable for a child." },
      },
      required: ["id", "title", "description", "tip"],
    },
  };

  if (!process.env.API_KEY) return fallbackDesignOptions;

  try {
    const ai = getAI();
    const response = await ai.models.generateContent({
      model: modelFlash,
      contents: `The user (a child) wants to build an app with this idea: "${idea}". 
      Suggest 3 distinct, creative visual themes/styles for this app. 
      Make the titles fun and catchy. 
      The tips should explain a design concept (like 'Contrast', 'Palette', 'Layout') simply.`,
      config: {
        responseMimeType: "application/json",
        responseSchema: schema,
        systemInstruction: "You are a friendly expert mentor for kids learning to code.",
      },
    });

    const text = response.text;
    if (!text) throw new Error("No response from AI");
    return JSON.parse(text) as OptionItem[];
  } catch (error) {
    console.error("Design Gen Error:", error);
    // Fallback if AI fails or key is missing
    return fallbackDesignOptions;
  }
};

export const generateLogicOptions = async (idea: string, design: string): Promise<OptionItem[]> => {
  const schema: Schema = {
    type: Type.ARRAY,
    items: {
      type: Type.OBJECT,
      properties: {
        id: { type: Type.STRING },
        title: { type: Type.STRING },
        description: { type: Type.STRING },
        tip: { type: Type.STRING, description: "A short educational fact about coding logic or game mechanics suitable for a child." },
      },
      required: ["id", "title", "description", "tip"],
    },
  };

  if (!process.env.API_KEY) return fallbackLogicOptions;

  try {
    const ai = getAI();
    const response = await ai.models.generateContent({
      model: modelFlash,
      contents: `The user is building a "${design}" style app about "${idea}". 
      Suggest 3 distinct gameplay or interaction mechanics. 
      How does the user interact with the app? 
      Make it fun and varied (e.g., clicking, dragging, typing, timing).`,
      config: {
        responseMimeType: "application/json",
        responseSchema: schema,
        systemInstruction: "You are a friendly expert mentor for kids learning to code.",
      },
    });

    const text = response.text;
    if (!text) throw new Error("No response from AI");
    return JSON.parse(text) as OptionItem[];
  } catch (error) {
    console.error("Logic Gen Error:", error);
    return fallbackLogicOptions;
  }
};

export const generateAppCode = async (idea: string, design: OptionItem, logic: OptionItem): Promise<string> => {
  if (!process.env.API_KEY) return createOfflineGame(idea, design, logic);

  try {
    const ai = getAI();
    const prompt = `
      Act as an expert web developer for kids.
      Create a SINGLE HTML file containing HTML, CSS, and JavaScript.
      
      APP REQUIREMENTS:
      - Concept: ${idea}
      - Visual Theme: ${design.title} (${design.description})
      - Mechanics: ${logic.title} (${logic.description})
      - Target Audience: Kids 6-12 years old.
      
      TECHNICAL CONSTRAINTS:
      - Use Tailwind CSS via CDN (already included in environment, but include the script tag in your output to be safe).
      - Use FontAwesome or similar via CDN if needed for icons.
      - NO external assets that require CORS or might break (images should be data URIs or placeholders like https://picsum.photos).
      - Make it visually impressive: animations, particles, bright colors.
      - Code must be robust and handle errors.
      - Ensure the app takes up the full window height/width.
      - IMPORTANT: Return ONLY raw HTML code. Do NOT wrap in markdown code blocks like \`\`\`html.
      
      EDUCATIONAL TWIST:
      - Add comments in the code explaining what complex parts do, as if teaching a kid.
    `;

    const response = await ai.models.generateContent({
      model: modelFlash,
      contents: prompt,
      config: {
        systemInstruction: "You are a world-class creative coder. Write clean, working, and impressive code.",
      }
    });

    let code = response.text || "";
    
    // Cleanup markdown if present (just in case)
    code = code.replace(/```html/g, '').replace(/```/g, '');
    
    return code;

  } catch (error) {
    console.error("Code Gen Error:", error);
    return createOfflineGame(idea, design, logic);
  }
};
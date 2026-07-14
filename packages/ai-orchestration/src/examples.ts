import { AiExamplesSchema, type AiExamples } from "@polyglotai/shared-types";
import { extractJson } from "./correction.js";
import { buildSystemPrompt, type LearnerContext } from "./policy.js";
import { renderTemplate } from "./template.js";
import { MalformedModelOutput, type AIProvider } from "./types.js";

/**
 * Default prompt for on-demand example sentences. Asks for level-appropriate, natural sentences
 * that actually use the word (not just define it), each with an English translation and an
 * optional short usage note. Returns strict JSON so a bad reply is caught by the schema below.
 */
export const DEFAULT_EXAMPLES_TEMPLATE = `
Give an adult {{targetLanguage}} learner {{count}} natural example sentences that use the word
or phrase "{{word}}" (meaning: {{meaning}}). Vary the situations. Match the learner's level from
the context below — simpler sentences for a beginner, richer ones for an advanced learner.

Respond with ONLY a JSON object (no prose, no code fences):
{
  "examples": [
    { "target": "<sentence in {{targetLanguage}}>", "translation": "<natural English translation>", "note": "<optional: register/nuance, omit if nothing to add>" }
  ]
}
`.trim();

export interface ExamplesEngineOptions {
  /** Pack-provided template overriding the built-in default (same mechanism as corrections). */
  template?: string;
}

/**
 * Generates a handful of fresh example sentences for one vocabulary item via the configured
 * provider — the Library's "show me this in real sentences" action. Same guarantees as
 * CorrectionEngine: the learner context + §13 content policy ride along, and the reply is
 * schema-validated so malformed output surfaces as MalformedModelOutput, never half-rendered.
 */
export class ExamplesEngine {
  constructor(
    private readonly provider: AIProvider,
    private readonly options: ExamplesEngineOptions = {},
  ) {}

  async examplesFor(word: string, meaning: string, ctx: LearnerContext, count = 3): Promise<AiExamples> {
    const template = this.options.template ?? DEFAULT_EXAMPLES_TEMPLATE;
    const task = renderTemplate(template, {
      targetLanguage: ctx.targetLanguage,
      word,
      meaning,
      count: String(count),
    });

    const { text: raw } = await this.provider.complete({
      messages: [
        { role: "system", content: buildSystemPrompt("You return only strict JSON.", ctx) },
        { role: "user", content: task },
      ],
      temperature: 0.7,
    });

    let parsed: unknown;
    try {
      parsed = JSON.parse(extractJson(raw));
    } catch {
      throw new MalformedModelOutput("model reply was not valid JSON", raw);
    }

    const result = AiExamplesSchema.safeParse(parsed);
    if (!result.success) {
      const issues = result.error.issues.map((i) => `${i.path.join(".")}: ${i.message}`).join("; ");
      throw new MalformedModelOutput(`model JSON did not match the examples schema (${issues})`, raw);
    }
    return result.data;
  }
}

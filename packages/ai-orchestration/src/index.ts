import type { AiCorrection } from "@polyglotai/shared-types";

/**
 * AIProvider — spec §8's provider-abstraction requirement. No external AI provider is
 * hard-coded; the OpenAI adapter (Milestone C step 11) is the only implementation in MVP.
 */
export interface AIProvider {
  readonly name: string;
  complete(request: AICompletionRequest): Promise<string>;
}

export interface AICompletionRequest {
  systemPrompt: string;
  messages: Array<{ role: "user" | "assistant"; content: string }>;
}

/**
 * AIOrchestrator builds prompts from pack ai-prompt templates + learner context, calls the
 * configured AIProvider, and parses structured output. The content-policy clause (spec §13:
 * academic explanation of vulgarity/slang/taboo is permitted; harassment, threats, sexual
 * exploitation, and wrongdoing instructions are refused) is injected here, not per-caller.
 */
export interface AIOrchestrator {
  correct(input: { text: string; learnerContext: unknown }): Promise<AiCorrection>;
}

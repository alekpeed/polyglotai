/**
 * Scheduler interface — spec §16. FSRS (via ts-fsrs) is the default implementation, built
 * out in Milestone B step 9; SM-2 is the documented fallback. Kept as an interface so
 * per-item-type scheduling policies can be swapped later (spec §9, plan risk 9).
 */
export interface ReviewGrade {
  /** 1=again 2=hard 3=good 4=easy */
  rating: 1 | 2 | 3 | 4;
}

export interface SchedulerState {
  difficulty: number;
  stability: number;
  retrievability: number;
  state: "new" | "learning" | "review" | "relearning";
  dueAt: string;
  lastReviewedAt: string | null;
  lapses: number;
  reps: number;
}

export interface Scheduler {
  initialState(): SchedulerState;
  schedule(current: SchedulerState, grade: ReviewGrade, now: Date): SchedulerState;
}

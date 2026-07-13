/** Prevents a second UI click from submitting the same review while the first save is pending. */
export function createSubmissionGuard() {
  let locked = false;

  return {
    async run(task: () => Promise<void>): Promise<boolean> {
      if (locked) return false;
      locked = true;
      try {
        await task();
        return true;
      } finally {
        locked = false;
      }
    },
  };
}

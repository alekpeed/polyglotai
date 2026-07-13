import { useEffect, useRef, useState } from "react";
import type { SpeechProvider } from "@polyglotai/pronunciation";

export type RecordPhase = "idle" | "recording" | "transcribing";

/** Mic capture + Whisper transcription, shared by any screen that wants a "speak your reply"
 * input alongside its text box (Conversation, Live Interpreter). Fills the caller's text field
 * rather than auto-submitting — a misheard transcript is easy to fix before sending, easy to
 * regret after. */
export function useVoiceRecorder(speechProvider: SpeechProvider | null) {
  const [phase, setPhase] = useState<RecordPhase>("idle");
  const [error, setError] = useState<string | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  useEffect(() => () => {
    recorderRef.current?.stream.getTracks().forEach((track) => track.stop());
    recorderRef.current = null;
  }, []);

  async function start(): Promise<void> {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      chunksRef.current = [];
      recorder.ondataavailable = (e) => chunksRef.current.push(e.data);
      recorder.start();
      recorderRef.current = recorder;
      setPhase("recording");
    } catch (err) {
      setError(`Microphone unavailable: ${String(err)}`);
    }
  }

  function stop(): Promise<string | null> {
    return new Promise((resolve) => {
      const recorder = recorderRef.current;
      if (!recorder) {
        resolve(null);
        return;
      }
      recorder.onstop = async () => {
        recorder.stream.getTracks().forEach((t) => t.stop());
        recorderRef.current = null;
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || "audio/webm" });
        if (!speechProvider) {
          setPhase("idle");
          resolve(null);
          return;
        }
        setPhase("transcribing");
        try {
          const text = await speechProvider.transcribe(blob);
          setPhase("idle");
          resolve(text);
        } catch (err) {
          setError(String(err));
          setPhase("idle");
          resolve(null);
        }
      };
      recorder.stop();
    });
  }

  return { phase, error, start, stop };
}

/** Plays a synthesized-speech blob and resolves once playback finishes. Errors (e.g. blocked
 * autoplay) reject rather than throw synchronously, so callers can swallow them without a
 * broken UI — TTS playback is a nice-to-have, never blocking for the text already on screen. */
export function playAudioBlob(blob: Blob): Promise<void> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(blob);
    const audio = new Audio(url);
    audio.onended = () => {
      URL.revokeObjectURL(url);
      resolve();
    };
    audio.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("Audio playback failed"));
    };
    void audio.play().catch(reject);
  });
}

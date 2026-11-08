export type RtmpPublisherStartOptions = {
  url: string;
  key: string;
  video: { codec: 'h264'; width: number; height: number; fps: number; bitrate: number };
  audio: { codec: 'aac'; bitrate: number; sampleRate: number; channels: number };
  cameraFacing: 'user' | 'environment';
  requireNativeCapture: true;
  overlay?: Record<string, unknown>;
};

export interface RtmpPublisherPlugin {
  start(options: RtmpPublisherStartOptions): Promise<{ state: 'live'; message?: string }>;
  stop(): Promise<{ state: 'stopped' }>;
  updateOverlay(options: Record<string, unknown>): Promise<void>;
}

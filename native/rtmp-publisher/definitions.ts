export type RtmpCodec = 'h264' | 'hevc';
export type RtmpPublisherStartOptions = {
  url: string; key: string;
  video: { codec: RtmpCodec; width: number; height: number; fps: number; bitrate: number };
  audio: { codec: 'aac'; bitrate: number; sampleRate: number; channels: number };
  cameraFacing: 'user' | 'environment'; requireNativeCapture: true;
  overlay?: Record<string, unknown>;
};
export interface RtmpPublisherPlugin {
  start(options: RtmpPublisherStartOptions): Promise<{ state: 'live'; codec: RtmpCodec; message?: string }>;
  stop(): Promise<{ state: 'stopped' }>;
  updateOverlay(options: Record<string, unknown>): Promise<void>;
  getCapabilities(): Promise<{ h264: boolean; hevc: boolean; rtmp: boolean; platform: string }>;
}

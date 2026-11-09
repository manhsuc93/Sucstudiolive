import { registerPlugin, type Plugin } from '@capacitor/core';

export type RtmpCodec = 'h264' | 'hevc';

export interface RtmpStartOptions {
  url: string;
  key: string;
  video: { codec: RtmpCodec; width: number; height: number; fps: number; bitrate: number };
  audio: { codec: 'aac'; bitrate: number; sampleRate: number; channels: number };
  cameraFacing: 'user' | 'environment';
  requireNativeCapture: true;
  overlay?: {
    banner?: { enabled?: boolean; title?: string; subtitle?: string; backgroundColor?: string };
    ticker?: { enabled?: boolean; text?: string };
    scoreboard?: { enabled?: boolean; teamA?: string; teamB?: string; scoreA?: number; scoreB?: number };
    logo?: null | { dataUrl: string; position: string };
  };
}

export interface RtmpPublisherPlugin extends Plugin {
  start(options: RtmpStartOptions): Promise<{ state: 'live'; codec: RtmpCodec; message?: string }>;
  stop(): Promise<{ state: 'stopped' }>;
  updateOverlay(options: RtmpStartOptions['overlay']): Promise<void>;
  getCapabilities(): Promise<{ h264: boolean; hevc: boolean; rtmp: boolean; platform: string }>;
}

export const RtmpPublisher = registerPlugin<RtmpPublisherPlugin>('RtmpPublisher');

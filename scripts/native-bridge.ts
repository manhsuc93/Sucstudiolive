import { RtmpPublisher } from '../native/rtmp-publisher/src/index';

if ((window as any).Capacitor?.isNativePlatform?.()) {
  (window as any).SucstudioliveRtmpPublisher = RtmpPublisher;
}

# Capacitor RtmpPublisher native plugin scaffold

This is a **native plugin shell**, not a working RTMP publisher yet. It is intentionally fail-closed: `start()` rejects with `RTMP_ENGINE_NOT_LINKED` until the HaishinKit engine adapter is implemented and tested.

## Capacitor 7 integration

1. From the repository root run `npm install`, then `npx cap add android` and `npx cap add ios` on a Mac for iOS.
2. Copy the Android Java source into the Android app module and register it with Capacitor's plugin registry.
3. Add the Swift source to the iOS app target and register it with the Capacitor bridge.
4. Add camera/microphone usage descriptions and runtime permissions.
5. Implement the engine adapter using HaishinKit before enabling live publishing.

## Current behavior

- `start`: validates input shape, then returns a clear error because the RTMP engine is not linked.
- `stop`: safe/idempotent no-op.
- `updateOverlay`: returns an explicit not-implemented error.
- No stream key is logged or persisted.

Do not distribute this scaffold as an RTMP-capable app.

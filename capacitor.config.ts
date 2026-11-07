import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'vn.livestudio.mobile',
  appName: 'Live Studio',
  webDir: 'www',
  bundledWebRuntime: false,
  android: {
    allowMixedContent: false,
    captureInput: true
  },
  ios: {
    contentInset: 'automatic',
    scrollEnabled: true
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1200,
      backgroundColor: '#0b1220',
      showSpinner: false
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#0b1220'
    },
    Keyboard: {
      resize: 'body',
      style: 'DARK'
    }
  }
};

export default config;

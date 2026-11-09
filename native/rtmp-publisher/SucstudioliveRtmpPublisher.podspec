Pod::Spec.new do |s|
  s.name         = 'SucstudioliveRtmpPublisher'
  s.version      = '0.0.1'
  s.summary      = 'RTMP Publisher plugin'
  s.homepage     = 'https://github.com/manhsuc93/Sucstudiolive'
  s.license      = 'MIT'
  s.authors      = { 'Author' => '' }
  s.source       = { :path => '.' }
  s.source_files = 'ios/Plugin/**/*.{swift,h,m}'
  s.dependency 'Capacitor'
  s.ios.deployment_target = '13.0'
end

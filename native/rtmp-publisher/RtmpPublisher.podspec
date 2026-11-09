Pod::Spec.new do |s|
  s.name = 'RtmpPublisher'
  s.version = '1.0.0'
  s.summary = 'Capacitor native RTMP publisher for Sucstudiolive'
  s.description = 'Native camera, microphone, H.264/HEVC, AAC and RTMP publishing bridge.'
  s.license = { :type => 'BSD-3-Clause' }
  s.homepage = 'https://github.com/manhsuc93/Sucstudiolive'
  s.author = 'Sucstudiolive'
  s.source = { :git => 'https://github.com/manhsuc93/Sucstudiolive.git', :tag => s.version.to_s }
  s.source_files = 'ios/**/*.{swift,h,m}'
  s.ios.deployment_target = '15.0'
  s.swift_version = '5.10'
  s.dependency 'Capacitor'
  s.dependency 'HaishinKit', '~> 1.9.1'
end

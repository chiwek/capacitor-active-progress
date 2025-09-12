Pod::Spec.new do |s|
  s.name             = 'CapacitorActiveProgress'
  s.version          = '0.1.0'
  s.summary          = 'Capacitor plugin for active progress (iOS Live Activity)'
  s.license          = 'MIT'
  s.homepage         = 'https://example.com'
  s.author           = 'Chiwek'
  s.source           = { :git => 'https://example.com/repo.git', :tag => s.version.to_s }
  s.source_files     = 'Plugin/**/*.{swift,h,m}'
  s.ios.deployment_target  = '14.0'
  s.swift_version     = '5.9'

  s.dependency 'Capacitor', '>= 6.0.0'
end

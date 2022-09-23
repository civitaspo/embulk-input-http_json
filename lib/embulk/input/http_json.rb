Embulk::JavaPlugin.register_input(
  "http_json", "org.embulk.input.http_json.HttpJsonInputPlugin",
  File.expand_path('../../../../classpath', __FILE__))

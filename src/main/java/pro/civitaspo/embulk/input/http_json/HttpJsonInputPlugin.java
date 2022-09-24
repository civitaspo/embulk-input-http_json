package pro.civitaspo.embulk.input.http_json;

import org.embulk.base.restclient.RestClientInputPluginBase;
import org.embulk.util.config.ConfigMapperFactory;
import pro.civitaspo.embulk.input.http_json.config.PluginTask;

public class HttpJsonInputPlugin extends RestClientInputPluginBase<PluginTask> {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();

    public HttpJsonInputPlugin() {
        super(
                CONFIG_MAPPER_FACTORY,
                PluginTask.class,
                new HttpJsonInputPluginDelegate(CONFIG_MAPPER_FACTORY));
    }
}

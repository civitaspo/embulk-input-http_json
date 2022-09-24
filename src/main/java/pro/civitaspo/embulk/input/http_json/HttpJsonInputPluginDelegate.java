package pro.civitaspo.embulk.input.http_json;

import java.util.List;
import org.embulk.base.restclient.RestClientInputPluginDelegate;
import org.embulk.base.restclient.ServiceDataSplitter;
import org.embulk.base.restclient.ServiceResponseMapper;
import org.embulk.base.restclient.record.RecordImporter;
import org.embulk.base.restclient.record.ValueLocator;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.json.JsonParser;
import pro.civitaspo.embulk.input.http_json.config.PluginTask;

public class HttpJsonInputPluginDelegate implements RestClientInputPluginDelegate<PluginTask> {

    private final ConfigMapperFactory ConfigMapperFactory;
    private final JsonParser jsonParser;

    public HttpJsonInputPluginDelegate(
            ConfigMapperFactory configMapperFactory, JsonParser jsonParser) {
        ConfigMapperFactory = configMapperFactory;
        this.jsonParser = jsonParser;
    }

    public HttpJsonInputPluginDelegate(ConfigMapperFactory configMapperFactory) {
        this(configMapperFactory, new JsonParser());
    }

    @Override
    public ConfigDiff buildConfigDiff(
            PluginTask task, Schema schema, int taskCount, List<TaskReport> taskReports) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void validateInputTask(PluginTask task) {
        // TODO Auto-generated method stub

    }

    @Override
    public TaskReport ingestServiceData(
            PluginTask task,
            RecordImporter recordImporter,
            int taskIndex,
            PageBuilder pageBuilder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceDataSplitter<PluginTask> buildServiceDataSplitter(PluginTask task) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceResponseMapper<? extends ValueLocator> buildServiceResponseMapper(
            PluginTask task) {
        // TODO Auto-generated method stub
        return null;
    }
}

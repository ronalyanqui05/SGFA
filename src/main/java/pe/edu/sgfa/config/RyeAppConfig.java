package pe.edu.sgfa.config;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_config")
public class RyeAppConfig {

    @Id
    private String configKey;

    private String configValue;

    protected RyeAppConfig() {
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getConfigValue() {
        return configValue;
    }
}

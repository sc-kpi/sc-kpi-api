package ua.kpi.sc.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

class ModuleInfoContributorTest {

    @Test
    @SuppressWarnings("unchecked")
    void contribute_addsModulesWithCountAndList() {
        var contributor = new ModuleInfoContributor();
        var builder = new Info.Builder();

        contributor.contribute(builder);

        Info info = builder.build();
        var modules = (Map<String, Object>) info.getDetails().get("modules");
        assertThat(modules).isNotNull();
        assertThat(modules.get("count")).isEqualTo(9);

        var list = (List<Map<String, String>>) modules.get("list");
        assertThat(list).hasSize(9);
        assertThat(list.getFirst().get("name")).isEqualTo("common");
        assertThat(list.getLast().get("name")).isEqualTo("feature-flag");

        list.forEach(entry -> {
            assertThat(entry).containsKeys("name", "package", "description");
            assertThat(entry.get("package")).startsWith("ua.kpi.sc.");
        });
    }
}

package systems.kinau.fishingbot.modules.ejection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import systems.kinau.fishingbot.gui.config.DisplayNameProvider;
import systems.kinau.fishingbot.utils.LocationUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class EjectionRule {

    private String name = "default";
    private LocationUtils.Direction direction = LocationUtils.Direction.SOUTH;
    private List<String> allowList = new ArrayList<>();
    private List<String> nbtAllowList = new ArrayList<>();
    private EjectionType ejectionType = EjectionType.DROP;

    public EjectionRule(String name, LocationUtils.Direction direction, List<String> allowList, EjectionType ejectionType) {
        this.name = name;
        this.direction = direction;
        this.allowList = allowList;
        this.ejectionType = ejectionType;
    }

    @AllArgsConstructor
    @Getter
    public enum EjectionType implements DisplayNameProvider {
        DROP("Drop Items"),
        FILL_CHEST("Fill Adjacent Chest");

        private final String displayName;
    }
}

package hai913i.tp1.model;

import java.util.List;

public record MethodFact(String id,
                         String name,
                         List<String> parameterTypes,
                         boolean constructor,
                         Visibility visibility,
                         int bodyLoc,
                         List<CallFact> calls) {

    public int parameterCount() {
        return parameterTypes.size();
    }
}

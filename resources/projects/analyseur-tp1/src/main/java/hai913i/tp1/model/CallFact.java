package hai913i.tp1.model;

import java.util.Optional;

public record CallFact(String callerId,
                       String calledName,
                       Optional<String> targetId,
                       String staticReceiverType,
                       int sourceLine,
                       boolean resolved,
                       boolean internal) {
}

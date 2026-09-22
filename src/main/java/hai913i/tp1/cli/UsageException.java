package hai913i.tp1.cli;

/** Ligne de commande invalide : message explicite, pas de pile d'exception. */
public class UsageException extends Exception {

    private static final long serialVersionUID = 1L;

    public UsageException(String message) {
        super(message);
    }
}

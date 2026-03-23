package model;

// VERRA' IMPLEMENTATA DALLE VIEW DI GUI ETC...
@FunctionalInterface
public interface IGameObserver {
    // Il gioco chiama questo metodo e passa se stesso (o lo stato rilevante)
    // così l'Observer (es. la UI) può leggere i nuovi dati e aggiornarsi.
    void onGameUpdated(Game game);
}
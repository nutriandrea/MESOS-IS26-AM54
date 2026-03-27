# MESOS - Regole di Gioco

## Panoramica

MESOS è un gioco di gestione risorse dove i giocatori vestono i panni di capi tribali, aggiungono membri alla tribù, costruiscono edifici e affrontano eventi.

**Vincitore**: Chi totalizza più Punti Prestigio (PP) alla fine.

---

## Preparazione

### Componenti
- **94 carte Tribù**: 84 Personaggi + 10 Eventi (divisi in Era I, II, III)
- **21 carte Edificio** (6 Era I, 7 Era II, 8 Era III)
- **7 tessere Offerta** (A-G)
- **1 tessera Ordine di Turno**
- **48 segnalini Cibo**
- **75 segnalini Punti Prestigio**
- **5 Totem**

### Setup
1. Formare tracciato Offerte (A→G in ordine alfabetico)
2. Posizionare tessera Ordine di Turno con lato corretto (numero giocatori)
3. Creare mazzo Tribù: in fondo Eventi Finali, poi Era III, Era II, Era I
4. Pescare `giocatori + 1` carte per fila inferiore
5. Pescare `giocatori + 4` carte per fila superiore
6. Posizionare edifici Era I nella fila superiore
7. Distribuire cibo iniziale: 1°=2, 2°-3°=3, 4°-5°=4

---

## Struttura del Round

Ogni round consiste in **2 fasi**:

### Fase 1: Piazzare i Totem
Secondo l'ordine sulla tessera Ordine di Turno (alto→basso), ogni giocatore sposta il proprio Totem su una tessera Offerta **libera**.

### Fase 2: Risoluzione Azioni
Dal giocatore con Totem più a sinistra verso destra:
1. Il giocatore esegue l'azione sulla propria tessera Offerta
2. Dopo l'azione, il Totem torna sulla tessera Ordine di Turno nel primo spazio libero
3. Se lo spazio ha bonus Cibo, lo ottiene immediatamente

---

## Tessere Offerta

Ogni tessera mostra:
- Numero di carte da prendere dalla **fila superiore** e/o **inferiore**
- Bonus Cibo iniziale (solo tessera A per 5 giocatori)

**Regole di pescata:**
- Non si possono mai prendere carte Evento
- Se non ci sono carte disponibili, non si ottiene nulla
- Se ci sono carte Personaggio disponibili, **devono** essere prese

---

## Carte Personaggio

Prendere Personaggi è **gratuito**.

### Tipi di Personaggio:

| Tipo | Effetto |
|------|---------|
| **Cacciatore** | Durante Caccia: +1 Cibo per ogni Cacciatore nella tribù |
| **Costruttore** | Riduce il costo degli Edifici (vedi sotto) |
| **Raccoglitore** | Durante Sostentamento: -3 Cibo dal totale da pagare |
| **Inventore** | A fine partita: (num Inventori) × (icone invenzione diverse) PP |
| **Artista** | Durante Pitture Rupestri: può guadagnare/perdere PP |
| **Sciamano** | Durante Rituale Sciamanico: determinano maggioranza/minoranza |

### Costi Edifici
Ogni Costruttore nella tribù riduce il costo di ogni Edificio del valore indicato. Gli sconti si sommano, **non possono mai portare il costo sotto 0**.

---

## Carte Edificio

Per prendere un Edificio si deve **pagare il costo in Cibo** (meno eventuali sconti dei Costruttori).

Se non si ha abbastanza Cibo, non si può prendere l'Edificio.

---

## Eventi

Gli eventi vengono risolti **alla fine di ogni round** dalla fila inferiore.

### Ordine di risoluzione:
1. Tutti gli eventi **tranne Sostentamento** (in ordine di Era)
2. **Sostentamento** per ultimo

### Tipi di Evento:

#### Caccia
- Prendi 1 Cibo per ogni Cacciatore nella tribù
- Guadagna PP indicati per ogni Cacciatore

#### Sostentamento
- Paga 1 Cibo per ogni Personaggio nella tribù (Edifici non contano)
- Ogni Raccoglitore dà -3 Cibo sul totale
- Se non hai abbastanza Cibo, perdi PP indicati per ogni Personaggio non sfamato

#### Pitture Rupestri
- Se hai ≤threshold Artisti: perdi PP indicati
- Se hai >threshold Artisti: guadagna PP × (Artisti - threshold)

#### Rituale Sciamanico
- Chi ha **più** icone Sciamano: guadagna PP indicati
- Chi ha **meno** icone Sciamano: perde PP indicati
- In caso di parità, tutti in parità ricevono/perdono PP

---

## Fine del Round

Alla fine di ogni round (dopo che tutti hanno completato le azioni):

1. **Risolvi Eventi** dalla fila inferiore (Sostentamento per ultimo)
2. **Scarta** tutte le carte dalla fila inferiore (Personaggi ed Eventi, **non** Edifici)
3. **Sposta** Personaggi ed Eventi dalla fila superiore alla fila inferiore
4. **Riempi** la fila superiore (`giocatori + 4` carte)

---

## Inizio di una Nuova Era

Quando si rivela una carta Tribù dell'Era successiva:

1. Se transizione a **Era III**: scarta eventuali Edifici dalla fila inferiore
2. Sposta gli Edifici dalla fila superiore alla fila inferiore
3. Piazza i nuovi Edifici dell'Era nella fila superiore

---

## Fine della Partita

La partita termina quando:
- Il mazzo Tribù è esaurito (round 10)

### Risoluzione finale:
1. Risolvi **TUTTI** gli eventi visibili (fila superiore + inferiore)
2. Sostentamento per ultimo

### Calcolo Punteggio Finale

Aggiungere ai PP totalizzati durante la partita:

| Componente | PP |
|------------|-----|
| PP dei **Costruttori** nella tribù | Somma dei valori indicati sulle carte |
| PP degli **Inventori** | (num Inventori) × (icone invenzione DISTINTE) |
| PP degli **Artisti** | (num Artisti / 2) × 10 |
| PP degli **Edifici** | Somma di finalAura di ogni Edificio + effetti endgame |

### Vincitore
1. Maggior PP totale vince
2. In caso di parità: più Cibo vince
3. Ulteriore parità: vittoria condivisa

---

## Riepilogo Flux Gioco

```
SetupGameState
    ↓ (tutti i giocatori hanno scelto colore)
StartTurnState
    ↓ (tutti hanno piazzato totem)
PickingState
    ↓ (ultimo giocatore finisce di pickare)
EventState
    ↓ (deck vuoto)
EndGameState
```

---

## Bonus Cibo TurnOrderSlot

Quando un giocatore torna sulla tessera Ordine di Turno:
- Se lo spazio ha bonus positivo: **ottiene subito quel Cibo**
- Se lo spazio ha bonus negativo (solo ultimo spazio): **paga quel Cibo** (o perde 2 PP se non ha Cibo)

---

## Effetti EndGame degli Edifici

Alcuni Edifici hanno effetti che si applicano a fine partita:
- **AURA_PER_JOB**: PP in base al numero di Personaggi di un tipo
- **DOUBLE_CONSTRUCTOR_AURA**: Doppio PP dai Costruttori
- **SIX_DIFFERENT_AURA**: 6 PP per ogni set completo di 6 tipi diversi
- **FLAT_AURA_BONUS**: PP fissi

---

## Variante Esperti

Nella variante esperti, tutti gli Edifici (Era I, II, III) sono **scoperti** fin dall'inizio della partita.

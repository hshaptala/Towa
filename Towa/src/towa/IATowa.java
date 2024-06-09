package towa;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Votre IA pour le jeu Towa.
 */
public class IATowa {

    /**
     * Hôte du grand ordonnateur.
     */
    String hote = null;

    /**
     * Port du grand ordonnateur.
     */
    int port = -1;

    /**
     * Couleur de votre joueur (IA) : 'N'oir ou 'B'lanc.
     */
    final char couleur;

    /**
     * Interface pour le protocole du grand ordonnateur.
     */
    TcpGrandOrdonnateur grandOrdo = null;

    /**
     * Nombre maximal de tours de jeu.
     */
    static final int NB_TOURS_JEU_MAX = 40;

    /**
     * La profondeur
     */
    static final int DEPTH = 4;

    /**
     *
     */
    static final int NB_ACTION_SELEC = 10;

    /**
     * Constructeur.
     *
     * @param hote Hôte.
     * @param port Port.
     * @param uneCouleur couleur du joueur
     */
    public IATowa(String hote, int port, char uneCouleur) {
        this.hote = hote;
        this.port = port;
        this.grandOrdo = new TcpGrandOrdonnateur();
        this.couleur = uneCouleur;
    }

    /**
     * Connexion au Grand Ordonnateur.
     *
     * @throws IOException exception sur les entrées/sorties
     */
    void connexion() throws IOException {
        System.out.print("Connexion au Grand Ordonnateur : " + hote + " " + port + "...");
        System.out.flush();
        grandOrdo.connexion(hote, port);
        System.out.println(" ok.");
        System.out.flush();
    }

    /**
     * Boucle de jeu : envoi des actions que vous souhaitez jouer, et réception
     * des actions de l'adversaire.
     *
     * @throws IOException exception sur les entrées/sorties
     */
    void toursDeJeu() throws IOException {
        // paramètres
        System.out.println("Je suis le joueur " + couleur + ".");
        // le plateau initial
        System.out.println("Réception du plateau initial...");
        Case[][] plateau = grandOrdo.recevoirPlateauInitial();
        System.out.println("Plateau reçu.");
        // compteur de tours de jeu (entre 1 et 40)
        int nbToursJeu = 1;
        // la couleur du joueur courant (change à chaque tour de jeu)
        char couleurTourDeJeu = Case.CAR_NOIR;
        // booléen pour détecter la fin du jeu
        boolean fin = false;
        while (!fin) {
            boolean disqualification = false;

            if (couleurTourDeJeu == couleur) {
                // à nous de jouer !
                jouer(plateau, nbToursJeu);
            } else {
                // à l'adversaire de jouer
                disqualification = adversaireJoue(plateau, couleurTourDeJeu);
            }
            if (nbToursJeu == NB_TOURS_JEU_MAX || disqualification) {
                // fini
                fin = true;
            } else {
                // au suivant
                nbToursJeu++;
                couleurTourDeJeu = suivant(couleurTourDeJeu);
            }
        }
    }

    /**
     * Fonction exécutée lorsque c'est à notre tour de jouer. Cette fonction
     * envoie donc l'action choisie au serveur.
     *
     * @param plateau le plateau de jeu
     * @param nbToursJeu numéro du tour de jeu
     * @throws IOException exception sur les entrées / sorties
     */
    void jouer(Case[][] plateau, int nbToursJeu) throws IOException {
        String actionJouee = actionChoisie(plateau, nbToursJeu);
        if (actionJouee != null) {
            // jouer l'action
            System.out.println("On joue : " + actionJouee);
            grandOrdo.envoyerAction(actionJouee);
            mettreAJour(plateau, actionJouee, couleur);
        } else {
            // Problème : le serveur vous demande une action alors que vous n'en
            // trouvez plus...
            System.out.println("Aucun action trouvée : abandon...");
            grandOrdo.envoyerAction("ABANDON");
        }
    }

    /**
     * Méthode qui renvoie l'action choisie par le joueur.
     *
     * @param plateau le plateau de jeu représenté par un tableau de cases.
     * @param nbToursJeu le nombre de tours de jeu.
     * @return l'action choisie par le joueur sous forme de chaîne de
     * caractères.
     */
    String actionChoisie(Case[][] plateau, int nbToursJeu) {
        JoueurTowa joueur = new JoueurTowa();
        String[] actionsPossibles = joueur.actionsPossibles(plateau, couleur, 6);
        if (nbToursJeu != 1) {
            actionsPossibles = selectAction(actionsPossibles, couleur);
        }
        actionsPossibles = enleverVitaliteTableau(actionsPossibles);
        return bestAction(plateau, actionsPossibles, DEPTH, nbToursJeu);
    }

    /**
     * Recherche et retourne la meilleure action à effectuer dans le jeu.
     *
     * @param plateau le plateau.
     * @param actionsPossibles les actions possibles.
     * @param depth la profondeur.
     * @param nbToursJeu le nombre de tours.
     * @return la meilleure action à effectuer.
     */
    String bestAction(Case[][] plateau, String[] actionsPossibles, int maxDepth, int nbToursJeu) {
        int alpha = -9999;
        int beta = 9999;
        String bestAction = null;
        Random r = new Random();
        if (nbToursJeu == 1) {
            for (String action : actionsPossibles) {
                System.out.print(action + " ");
            }
            int random = r.nextInt(actionsPossibles.length);
            System.out.println(random);
            return actionsPossibles[random];
        }
        for (int depth = 1; depth <= maxDepth; depth++) {
            int bestScore = -9999;
            String bestActionForDepth = null;
            Case[][] copiePlateau = copierPlateau(plateau);
            for (String action : actionsPossibles) {
                Case[][] plateauAModifier = copierPlateau(copiePlateau);
                mettreAJour(plateauAModifier, action, couleur);
                int score = minMax(plateauAModifier, true, depth, 0, alpha, beta);
                if (score > bestScore) {
                    bestActionForDepth = action;
                    bestScore = score;
                }
            }
            if (bestActionForDepth != null) {
                bestAction = bestActionForDepth;
            }
        }
        return bestAction;
    }

    /**
     * Calcule le score optimal pour un joueur donné à partir d'un plateau.
     *
     * @param plateau le plateau.
     * @param maximising indique si le joueur est en train de maximiser ou
     * minimiser le score.
     * @param alpha la valeur de l'alpha.
     * @param beta la valeur du bêta.
     * @param currentDepth la profondeur actuelle.
     * @param totalDepth la profondeur totale.
     */
    int minMax(Case[][] plateau, boolean maximising, int alpha, int beta, int currentDepth, int totalDepth) {
        JoueurTowa joueur = new JoueurTowa();
        String[] actionsPossibles;
        if (currentDepth >= totalDepth) {
            return evaluerPlateau(plateau);
        }
        if (maximising) {
            int score = -9999;
            actionsPossibles = joueur.actionsPossibles(plateau, couleur, 6);
            actionsPossibles = selectAction(actionsPossibles, couleur);
            actionsPossibles = enleverVitaliteTableau(actionsPossibles);
            for (String action : actionsPossibles) {
                Case[][] plateauCopie = copierPlateau(plateau);
                mettreAJour(plateauCopie, action, couleur);
                score = Math.max(score, minMax(plateauCopie, false, totalDepth, currentDepth + 1, alpha, beta));
                alpha = Math.max(alpha, score);
                if (score >= beta) {
                    break;
                }
            }
            return score;
        } else {
            int score = 9999;
            actionsPossibles = joueur.actionsPossibles(plateau, couleur, 6);
            actionsPossibles = selectAction(actionsPossibles, couleur);
            actionsPossibles = enleverVitaliteTableau(actionsPossibles);
            for (String action : actionsPossibles) {
                Case[][] plateauCopie = copierPlateau(plateau);
                mettreAJour(plateauCopie, action, couleur);
                score = Math.min(score, minMax(plateauCopie, true, totalDepth, currentDepth + 1, alpha, beta));
                beta = Math.min(beta, score);
                if (score <= alpha) {
                    break;
                }
            }
            return score;
        }
    }

    /**
     * Évalue le plateau de jeu en fonction du nombre de pions présents.
     *
     * @param plateau le plateau.
     */
    int evaluerPlateau(Case[][] plateau) {
        NbPions nbPions = JoueurTowa.nbPions(plateau);
        if (couleur == Case.CAR_NOIR) {
            return nbPions.nbPionsNoirs - nbPions.nbPionsBlancs;
        } else {
            return nbPions.nbPionsBlancs - nbPions.nbPionsNoirs;
        }
    }

    /**
     * Sélectionne les actions possibles en fonction de la couleur donnée.
     *
     * @param actionsPossibles les actions possibles.
     * @param couleur la couleur.
     * @return un tableau contenant les actions sélectionnées.
     */
    static String[] selectAction(String[] actionsPossibles, char couleur) {
        int[] scores = new int[actionsPossibles.length];
        for (int i = 0; i < actionsPossibles.length; i++) {
            scores[i] = scoreAction(actionsPossibles[i], couleur);
        }
        for (int i = 0; i < scores.length - 1; i++) {
            for (int j = i + 1; j < scores.length; j++) {
                if (scores[i] < scores[j]) {
                    int tempScore = scores[i];
                    scores[i] = scores[j];
                    scores[j] = tempScore;
                    String tempAction = actionsPossibles[i];
                    actionsPossibles[i] = actionsPossibles[j];
                    actionsPossibles[j] = tempAction;
                }
            }
        }
        String[] actionsSelectionnees = new String[NB_ACTION_SELEC * 2];
        int selectionIndex = 0;
        for (int i = 0; i < NB_ACTION_SELEC; i++) {
            if (selectionIndex < actionsPossibles.length) {
                actionsSelectionnees[i] = actionsPossibles[selectionIndex++];
            }
            if (selectionIndex < actionsPossibles.length) {
                actionsSelectionnees[NB_ACTION_SELEC + i] = actionsPossibles[selectionIndex++];
            }
        }
        return actionsSelectionnees;
    }

    /**
     * Calcule le score pour une action donnée.
     *
     * @param action l'action pour laquelle calculer le score.
     * @param couleur la couleur.
     * @return le score calculé.
     */
    static int scoreAction(String action, char couleur) {
        int[] numbers = getVitality(action);
        switch (couleur) {
            case Case.CAR_NOIR:
                return numbers[0] - numbers[1];
            case Case.CAR_BLANC:
                return numbers[1] - numbers[0];
            default:
                return 0;
        }
    }

    /**
     * Récupère la vitalité à partir d'une action donnée.
     *
     * @param action l'action contenant les informations de vitalité.
     * @return un tableau d'entiers.
     */
    static int[] getVitality(String action) {
        String[] parts = action.split(",");
        int[] numbers = new int[2];
        numbers[0] = Integer.parseInt(parts[1]);
        numbers[1] = Integer.parseInt(parts[2]);
        return numbers;
    }

    /**
     * Retire les vitalités de chaque action dans le tableau donné.
     *
     * @param actions le tableau d'actions contenant les vitalités à retirer.
     * @return un nouveau tableau contenant les actions sans les vitalités.
     */
    static String[] enleverVitaliteTableau(String[] actions) {
        String[] result = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            result[i] = ActionsPossibles.enleverVitalites(actions[i]);
        }
        return result;
    }

    /**
     * Copie le plateau de jeu en créant une nouvelle instance de tableau de
     * cases.
     *
     * @param plateau le plateau de jeu à copier.
     * @return le nouveau plateau de jeu copié.
     */
    static Case[][] copierPlateau(Case[][] plateau) {
        Case[][] newPlateau = new Case[Coordonnees.NB_LIGNES][Coordonnees.NB_COLONNES];
        for (int i = 0; i < plateau.length; i++) {
            for (int j = 0; j < plateau[0].length; j++) {
                Case caseInitiale = plateau[i][j];
                Case copieCase = new Case(caseInitiale.couleur, caseInitiale.hauteur, 0, Case.CAR_TERRE);
                newPlateau[i][j] = copieCase;
            }
        }
        return newPlateau;
    }

    boolean adversaireJoue(Case[][] plateau, char couleurAdversaire) {
        boolean disqualification = false;
        System.out.println("Attente de réception action adversaire...");
        String actionAdversaire = grandOrdo.recevoirAction();
        System.out.println("Action adversaire reçue : " + actionAdversaire);
        if ("Z".equals(actionAdversaire)) {
            System.out.println("L'adversaire est disqualifié.");
            disqualification = true;
        } else {
            System.out.println("L'adversaire joue : " + actionAdversaire + ".");
            mettreAJour(plateau, actionAdversaire, couleurAdversaire);
        }
        return disqualification;
    }

    static char suivant(char couleurCourante) {
        return couleurCourante == Case.CAR_NOIR
                ? Case.CAR_BLANC : Case.CAR_NOIR;
    }

    /**
     * Mettre à jour le plateau suite à une action, supposée valide.
     *
     * @param plateau le plateau.
     * @param action l'action à appliquer.
     * @param couleurCourante couleur du joueur courant.
     */
    static void mettreAJour(Case[][] plateau, String action,
            char couleurCourante) {
        // vérification des arguments
        if (plateau == null || action == null || action.length() != 3) {
            return;
        }
        Coordonnees coord = Coordonnees.depuisCars(action.charAt(1), action.charAt(2));
        switch (action.charAt(0)) {
            case 'P':
                poser(coord, plateau, couleurCourante);
                break;
            case 'A':
                activer(coord, plateau, couleurCourante);
                break;
            default:
                System.out.println("Type d'action incorrect : " + action.charAt(0));
        }
    }

    /**
     * Poser un pion sur une case donnée (vide ou pas).
     *
     * @param coord coordonnées de la case
     * @param plateau le plateau de jeu
     * @param couleurCourante couleur du joueur courant
     */
    static void poser(Coordonnees coord, Case[][] plateau, char couleur) {
        Case laCase = plateau[coord.ligne][coord.colonne];
        if (laCase != null) {
            if (laCase.tourPresente()) {
                laCase.hauteur++;
            } else {
                laCase.couleur = couleur;
                if (ennemiVoisine(coord, plateau, couleur)) {
                    laCase.hauteur = 2;
                } else {
                    laCase.hauteur = 1;
                }
            }
        }
    }

    static void activer(Coordonnees coord, Case[][] plateau, char couleurCourante) {
        final int hauteurTourJoueur = plateau[coord.ligne][coord.colonne].hauteur;
        List<Case> aDetruire
                = porteeActivation(coord)
                        .map(aPortee -> plateau[aPortee.ligne][aPortee.colonne])
                        .filter(c -> c.tourPresente()) // une tour
                        .filter(c -> c.couleur != couleurCourante) // ennemie
                        .filter(c -> c.hauteur < hauteurTourJoueur) // plus basse
                        .collect(Collectors.toList());
        for (Case tourADetruire : aDetruire) {
            detruireTour(tourADetruire);
        }
    }

    /**
     * Coordonnées des cases à portée d'activation.
     *
     * @param coord les coordonnées de la case activée
     * @return les coordonnées des cases à portée d'activation
     */
    static Stream<Coordonnees> porteeActivation(final Coordonnees coord) {
        return Stream.concat(voisines(coord),
                Stream.concat(memeLigne(coord), memeColonne(coord)));
    }

    /**
     * Les coordonnées des cases sur la même ligne (sans celles de la case
     * d'origine).
     *
     * @param coord les coordonnées de la case d'origine
     * @return les coordonnées des cases sur la même ligne
     */
    static Stream<Coordonnees> memeLigne(final Coordonnees coord) {
        return IntStream.rangeClosed(0, Coordonnees.NB_LIGNES - 1).boxed()
                .filter(col -> col != coord.colonne)
                .map(col -> new Coordonnees(coord.ligne, col));
    }

    /**
     * Les coordonnées des cases sur la même colonne (sans celles de la case
     * d'origine).
     *
     * @param coord les coordonnées de la case d'origine
     * @return les coordonnées des cases sur la même colonne
     */
    static Stream<Coordonnees> memeColonne(final Coordonnees coord) {
        return IntStream.rangeClosed(0, Coordonnees.NB_COLONNES - 1).boxed()
                .filter(lig -> lig != coord.ligne)
                .map(lig -> new Coordonnees(lig, coord.colonne));
    }

    static void detruireTour(Case laCase) {
        laCase.hauteur = 0;
        laCase.couleur = Case.CAR_VIDE;
    }

    /**
     * Indique si une case possède une case voisine avec une tour ennemie.
     *
     * @param coord la case dont on souhaite analyser les voisines
     * @param plateau le plateau courant
     * @param couleurCourante couleur du joueur courant
     * @return vrai ssi la case possède une voisine avec une tour ennemie
     */
    static boolean ennemiVoisine(Coordonnees coord, Case[][] plateau, char couleurCourante) {
        return voisines(coord)
                .map(v -> plateau[v.ligne][v.colonne])
                .filter(c -> c.tourPresente())
                .anyMatch(c -> c.couleur != couleurCourante);
    }

    /**
     * Les coordonnées des cases voisines dans le plateau.
     *
     * @param coord les coordonnées de la case d'origine
     * @return les coordonnées des cases voisines
     */
    static Stream<Coordonnees> voisines(final Coordonnees coord) {
        return IntStream.rangeClosed(-1, 1).boxed()
                .flatMap(l -> IntStream.rangeClosed(-1, 1)
                .filter(c -> !(l == 0 && c == 0))
                .mapToObj(c -> new Coordonnees(coord.ligne + l, coord.colonne + c)))
                .filter(v -> 0 <= v.ligne && v.ligne < Coordonnees.NB_LIGNES)
                .filter(v -> 0 <= v.colonne && v.colonne < Coordonnees.NB_COLONNES);
    }

    public static void main(String[] args) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        System.out.println("Démarrage le " + format.format(new Date()));
        System.out.flush();
        final String USAGE = System.lineSeparator() + "\tUsage : java " + IATowa.class.getName() + " <hôte> <port> <ordre>";
        if (args.length != 3) {
            System.out.println("Nombre de paramètres incorrect." + USAGE);
            System.out.flush();
            System.exit(1);
        }
        String hote = args[0];
        int port = -1;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Le port doit être un entier." + USAGE);
            System.out.flush();
            System.exit(1);
        }
        int ordre = -1;
        try {
            ordre = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("L'ordre doit être un entier." + USAGE);
            System.out.flush();
            System.exit(1);
        }
        try {
            char couleurJoueur = (ordre == 1 ? 'N' : 'B');
            IATowa iaTowa = new IATowa(hote, port, couleurJoueur);
            iaTowa.connexion();
            iaTowa.toursDeJeu();
        } catch (IOException e) {
            System.out.println("Erreur à l'exécution du programme : \n" + e);
            System.out.flush();
            System.exit(1);
        }
    }
}

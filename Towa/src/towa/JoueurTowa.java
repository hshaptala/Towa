package towa;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Joueur implémentant les actions possibles à partir d'un plateau, pour un
 * niveau donné.
 */
public class JoueurTowa implements IJoueurTowa {

    /**
     * Cette méthode renvoie, pour un plateau donné et un joueur donné, toutes
     * les actions possibles pour ce joueur.
     *
     * @param plateau le plateau considéré
     * @param couleurJoueur couleur du joueur
     * @param niveau le niveau de la partie à jouer
     * @return l'ensemble des actions possibles
     */
    @Override
    public String[] actionsPossibles(Case[][] plateau, char couleurJoueur, int niveau) {
        // afficher l'heure de lancement
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        System.out.println("actionsPossibles : lancement le " + format.format(new Date()));
        // se préparer à stocker les actions possibles
        ActionsPossibles actions = new ActionsPossibles();
        // on compte le nombre de pions sur le plateau avant action
        NbPions nbPions = nbPions(plateau);
        // pour chaque ligne
        for (int lig = 0; lig < Coordonnees.NB_LIGNES; lig++) {
            // pour chaque colonne
            for (int col = 0; col < Coordonnees.NB_COLONNES; col++) {
                Coordonnees coord = new Coordonnees(lig, col);
                // si la pose d'un pion de cette couleur est possible sur cette case
                if (posePossible(plateau, coord, couleurJoueur)) {
                    if (posePossible2Pions(plateau, coord, couleurJoueur)) {
                        ajoutActionPose(coord, actions, nbPions, couleurJoueur, 2);
                    } else {
                        // on ajoute l'action dans les actions possibles
                        ajoutActionPose(coord, actions, nbPions, couleurJoueur, 1);
                    }
                }
                // si l'attaque est possible sur cette case
                if (attackPossible(plateau, coord, couleurJoueur)) {
                    // on ajoute l'action dans les actions possibles
                    ajoutActionAttack(coord, actions, nbPions, couleurJoueur, plateau);
                }
            }
        }
        System.out.println("actionsPossibles : fin");
        return actions.nettoyer();
    }

    /**
     * Indique s'il est possible de poser un pion sur une case pour ce plateau,
     * ce joueur, dans ce niveau.
     *
     * @param plateau le plateau
     * @param coord coordonnées de la case à considérer
     * @param couleur couleur du joueur
     * @return vrai si la pose d'un pion sur cette case est autorisée dans ce
     * niveau
     */
    static boolean posePossible(Case[][] plateau, Coordonnees coord, char couleur) {
        Case pose = plateau[coord.ligne][coord.colonne];
        // la condition dans laquelle un pion peut être placé
        return !pose.tourPresente() || (pose.tourPresente()
                && pose.couleur == couleur && pose.hauteur < 4);
    }

    /**
     * Indique si la pose de deux pions est possible sur une case pour ce
     * plateau, ce joueur, dans ce niveau.
     *
     * @param plateau le plateau
     * @param coord coordonnées de la case à considérer
     * @param couleur couleur du joueur
     * @return vrai si la pose de deux pions est possible dans ce niveau
     */
    static boolean posePossible2Pions(Case plateau[][], Coordonnees coord, char couleur) {
        Case pose = plateau[coord.ligne][coord.colonne];
        Coordonnees[] voisins = voisines(coord);
        boolean posePossible = false;
        for (Coordonnees voisinCoord : voisins) {
            Case voisinCase = plateau[voisinCoord.ligne][voisinCoord.colonne];
            // la condition dans laquelle 2 pions peuvent être placés
            if (!pose.tourPresente() && voisinCase.tourPresente()
                    && voisinCase.couleur != couleur) {
                posePossible = true;
            }
        }
        return posePossible;
    }

    /**
     * Indique s'il est possible d'attaquer les pions ennemies pour ce plateau,
     * ce joueur, dans ce niveau.
     *
     * @param plateau le plateau
     * @param coord coordonnées de la case à considérer
     * @param couleur couleur du joueur
     * @return vrai si l'attaque est autorisée dans ce niveau
     */
    static boolean attackPossible(Case[][] plateau, Coordonnees coord, char couleur) {
        Case attack = plateau[coord.ligne][coord.colonne];
        return attack.tourPresente() && attack.couleur == couleur;
    }

    /**
     * Nombre de pions sur le plateau, de chaque couleur.
     *
     * @param plateau le plateau
     * @return le nombre de pions sur le plateau, de chaque couleur
     */
    static NbPions nbPions(Case[][] plateau) {
        int nbPionsNoirs = 0;
        int nbPionsBlancs = 0;
        for (int lig = 0; lig < Coordonnees.NB_LIGNES; lig++) {
            for (int col = 0; col < Coordonnees.NB_COLONNES; col++) {
                if (plateau[lig][col].couleur == Case.CAR_NOIR) {
                    nbPionsNoirs += plateau[lig][col].hauteur;
                } else if (plateau[lig][col].couleur == Case.CAR_BLANC) {
                    nbPionsBlancs += plateau[lig][col].hauteur;
                }
            }
        }
        return new NbPions(nbPionsNoirs, nbPionsBlancs);
    }

    /**
     * Ajout d'une action de pose dans l'ensemble des actions possibles.
     *
     * @param coord coordonnées de la case où poser un pion
     * @param actions l'ensemble des actions possibles
     * @param nbPions le nombre de pions par couleur sur le plateau avant de
     * jouer l'action
     * @param couleur la couleur du pion à ajouter
     */
    static void ajoutActionPose(Coordonnees coord, ActionsPossibles actions,
            NbPions nbPions, char couleur, int n) {
        String action = "P" + coord.carLigne() + coord.carColonne() + ","
                + (nbPions.nbPionsNoirs) + ","
                + (nbPions.nbPionsBlancs);

        // si la couleur d'un pion est noir
        if (couleur == Case.CAR_NOIR) {
            action = "P" + coord.carLigne() + coord.carColonne() + ","
                    + (nbPions.nbPionsNoirs + n) + ","
                    + (nbPions.nbPionsBlancs);
        }
        // si la couleur d'un pion est blanc
        if (couleur == Case.CAR_BLANC) {
            action = "P" + coord.carLigne() + coord.carColonne() + ","
                    + (nbPions.nbPionsNoirs) + ","
                    + (nbPions.nbPionsBlancs + n);
        }

        actions.ajouterAction(action);
    }

    /**
     * Ajout d'une action d'attaque dans l'ensemble des actions possibles.
     *
     * @param coord coordonnées de la case où activer la tour
     * @param actions l'ensemble des actions possibles
     * @param nbPions le nombre de pions par couleur sur le plateau avant de
     * jouer l'action
     * @param couleur la couleur du pion qui attaque
     */
    static void ajoutActionAttack(Coordonnees coord, ActionsPossibles actions,
            NbPions nbPions, char couleur, Case[][] plateau) {
        String action = "A" + coord.carLigne() + coord.carColonne() + ","
                + (nbPions.nbPionsNoirs) + ","
                + (nbPions.nbPionsBlancs);
        // les coordonnées du joueur attaquant
        Case joueur = plateau[coord.ligne][coord.colonne];
        // la hauteur à soustraire
        int hauteur = 0;

        // les conditions pour les cases voisines
        for (Coordonnees voisinCoord : voisines(coord)) {
            Case voisinCase = plateau[voisinCoord.ligne][voisinCoord.colonne];
            if (voisinCase.tourPresente() && voisinCase.couleur != couleur
                    && voisinCase.hauteur < joueur.hauteur) {
                hauteur += voisinCase.hauteur;
            }
        }

        // les conditions pour toutes les cases sur la même ligne et colonne
        for (Coordonnees c : toursLigCol(coord, plateau)) {
            Case tour = plateau[c.ligne][c.colonne];
            if (tour.tourPresente() && tour.couleur != couleur
                    && tour.hauteur < joueur.hauteur) {
                hauteur += tour.hauteur;
            }
        }

        // si la couleur d'un pion est noir
        if (couleur == Case.CAR_NOIR) {
            action = "A" + coord.carLigne() + coord.carColonne() + ","
                    + (nbPions.nbPionsNoirs) + ","
                    + (nbPions.nbPionsBlancs - hauteur);
        // si la couleur d'un pion est blanc
        } else if (couleur == Case.CAR_BLANC) {
            action = "A" + coord.carLigne() + coord.carColonne() + ","
                    + (nbPions.nbPionsNoirs - hauteur) + ","
                    + (nbPions.nbPionsBlancs);
        }

        actions.ajouterAction(action);
    }

    /**
     * Indique si ces coordonnées sont dans le plateau.
     *
     * @param coord coordonnées à tester
     * @param taille taille du plateau (carré)
     * @return vrai si ces coordonnées sont dans le plateau
     */
    static boolean estDansPlateau(Coordonnees coord) {
        return !(coord.ligne < 0 || coord.ligne >= Coordonnees.NB_LIGNES
                || coord.colonne < 0 || coord.colonne >= Coordonnees.NB_COLONNES);
    }

    /**
     * Renvoie les coordonnées de la case suivante, en suivant une direction
     * donnée.
     *
     * @param d la direction à suivre
     * @return les coordonnées de la case suivante
     */
    static Coordonnees suivante(Coordonnees c, Direction d) {
        return new Coordonnees(c.ligne + Direction.mvtVertic(d),
                c.colonne + Direction.mvtHoriz(d));
    }

    /**
     * Retourne les coordonnées de toutes les cases voisines.
     *
     * @param coord coordonnées de la case considérée
     * @return coordonnées de toutes les cases voisines
     */
    static Coordonnees[] voisines(Coordonnees coord) {
        Coordonnees[] voisines = new Coordonnees[8];
        int nbVoisines = 0;
        for (Direction d : Direction.values()) {
            Coordonnees c = suivante(coord, d);
            if (estDansPlateau(c)) {
                voisines[nbVoisines] = c;
                nbVoisines++;
            }
        }
        return Arrays.copyOf(voisines, nbVoisines);
    }

    /**
     * Renvoie les coordonnées de toutes les cases sur la même ligne et colonne.
     *
     * @param plateau le plateau
     * @param coord coordonnées de la case à considérer
     * @param couleur couleur du joueur
     * @return coordonnées de toutes les cases sur la même ligne et colonne
     */
    static Coordonnees[] toursLigCol(Coordonnees coord, Case[][] plateau) {
        Coordonnees[] tourCoord = new Coordonnees[32];
        int nbTours = 0;
        for (Direction d : Direction.cardinales()) {
            Coordonnees tmp = suivante(coord, d);
            Coordonnees c = suivante(tmp, d);
            while (estDansPlateau(c)) {
                /*
                Niveau 7(fonctionne pas)
                
                Case tour = plateau[c.ligne][c.colonne];
                if (tour.tourPresente()) {
                    tourCoord[nbTours] = c;
                    break;
                }
                 */
                tourCoord[nbTours] = c;
                nbTours++;
                c = suivante(c, d);
            }
        }
        return Arrays.copyOf(tourCoord, nbTours);
    }

}

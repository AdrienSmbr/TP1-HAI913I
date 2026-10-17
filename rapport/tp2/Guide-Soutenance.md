# Calculs et explications à maîtriser avant remise

Ce guide fournit les réponses techniques vérifiables dans le code. Il ne remplace pas leur appropriation par les deux membres du binôme.

1. **Pourquoi diviser par 37 ?** Le graphe contient 44 arcs uniques de méthode à méthode. Sept sont internes à une classe, donc exclus du couplage. Il reste 37 arcs interclasses. Les 38 sites interclasses mesurent autre chose et ne sont pas le dénominateur imposé.

2. **Pourquoi la somme vaut-elle 1 et non 2 ?** Chaque arc interclasse contribue à une seule paire non ordonnée de classes. La somme de ses numérateurs vaut donc 37. La matrice entière contient deux fois chaque paire, tandis que le graphe non orienté la contient une fois.

3. **Quel effet ont les zéros dans le lien moyen ?** Dans le micro jeu, C(A,B)=2/3, C(B,C)=1/3, C(A,C)=0. Après fusion A,B, la similarité avec C est (0+1/3)/2=1/6. Oublier le zéro donnerait à tort 1/3.

4. **Comment trancher une égalité ?** Les feuilles sont triées par nom qualifié. Les fusions reçoivent les identifiants suivants. On calcule d'abord le maximum, puis on choisit la plus petite paire d'identifiants à moins de 10⁻⁹ de ce maximum. Ce choix est reproductible, sans justification métier particulière.

5. **Pourquoi quatorze fusions ?** On part de quinze groupes ; chaque fusion en supprime deux et en ajoute un, soit un groupe de moins. Quatorze étapes aboutissent à une racine. Les fusions à zéro restent nécessaires pour terminer l'arbre et ne prouvent aucune responsabilité commune.

6. **Quelle différence entre similarité et moyenne interne ?** S(G1,G2) porte sur les couples entre les deux fils. Cbar(G) porte sur tous les couples des membres du groupe. Le noyau de sept classes à CP=0,025 a Cbar=26/(21×37)=0,0334620335, alors que sa fusion s'est faite à 0,0090090090.

7. **Que fait la coupe ?** À la racine, retenir le groupe si sa moyenne interne est strictement supérieure à CP ; sinon descendre dans les deux fils. Une feuille est toujours retenue. La limite floor(M/2) est vérifiée ensuite ; aucune fusion arbitraire ne répare une coupe rejetée.

8. **Quels résultats pour CP ?** Bibliothèque, lien moyen : 0,015 donne quatre modules dont un de onze classes ; 0,025 donne sept modules ; 0,045 en donne dix, donc rejet. À l'égalité exacte entre moyenne interne et CP, le groupe est refusé.

9. **Pourquoi Member–ConsoleNotifier ?** L'appel de send à getName crée leur arête de 1/37. À cette étape, les autres voisins de Member appartiennent déjà à des groupes plus grands ; leurs moyennes sont diluées. La fusion est explicable numériquement mais reste discutable sémantiquement.

10. **Pourquoi Dvd et LoanException isolées ?** Hériter, construire et lever une exception ne sont pas des appels de méthode retenus. L'algorithme reçoit donc deux nœuds sans arête dans le modèle obligatoire. Leur héritage ne doit pas être ajouté silencieusement.

11. **À quelle étape divergent lien moyen et lien simple ?** À la deuxième. Le lien simple préfère Loan au groupe Item–LoanService grâce au maximum 4/37. La moyenne de leurs deux couples vaut seulement 2/37, donc le lien moyen préfère LibraryApp–Catalog à 3/37.

12. **Que change C2 ?** Quatre relations internes s'ajoutent aux 37 arcs. Le dénominateur devient 41 et le graphe a 22 arêtes. À CP=0,025, huit modules sont rejetés ; à 0,020, sept sont acceptés. Séparer l'effet des nouvelles arêtes de celui de la nouvelle échelle est nécessaire.

13. **Que prouve C4 ?** Le même moteur fonctionne sur des faits reconstruits par le TP1 : 30 classes, 199 méthodes, 267 arcs, dont 190 interclasses. Le seuil 0,025 produit 23 modules rejetés ; 0,010 en produit quinze acceptés. Le corpus et les commandes sont fournis.

14. **Comment les méthodes implicites sont-elles traitées ?** Le pont réintègre les cibles internes déjà résolues par JDT mais absentes des méthodes déclarées : 69 méthodes, 121 arcs et 144 sites (143 vers RECORD, un vers ENUM). Voir cibles-reintegrees.csv. Il ne reconstruit pas les corps générés. La normalisation change de 69 à 190 arcs interclasses et la composition des modules change, même si leur nombre reste quinze à CP=0,010.

15. **Comment sait-on que le code n'est pas figé pour les exemples ?** Un oracle distinct en fractions exactes compare 112 exécutions, dont 16 systèmes générés à données mélangées. Il vérifie les valeurs, l'ordre des fusions, les membres, les coupes et les codes ; il n'appelle pas les fonctions Java testées pour calculer ses résultats attendus.

16. **Que fait l'interface sans dupliquer le moteur ?** ExplorerModel appelle le même regroupement et la même coupe. La recherche et le filtre d'arêtes modifient seulement l'affichage. L'export partage le même générateur de résumé que la CLI. Le code de sortie à la fermeture correspond à la coupe courante.

17. **Peut-on appeler les modules des composants ou des microservices ?** Le calcul produit des branches retenues. Pour parler de composants candidats, il faudrait attribuer et justifier des responsabilités et des interfaces. Le déploiement indépendant n'est pas établi par ces faits.

Pour la lecture du code : commencer par Main, suivre FactsSource → CouplingCalculator → WeightedGraph → AgglomerativeClusterer → ModuleCutter, puis lire AnalysisReport et Exports. Examiner ensuite ExplorerModel pour constater la réutilisation du calcul et ExportTp2 pour identifier la frontière avec JDT.

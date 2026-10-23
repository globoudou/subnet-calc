# Subnet — calculatrice de sous-réseaux IP pour Android

Application Android (Kotlin + Jetpack Compose) pensée pour un usage au pouce.

## Fonctions

**IPv4**
- Saisie tolérante : `192.168.1.10/24`, `192.168.1.10 255.255.255.0`, `192.168.1.10/255.255.255.0`, `10.0.0.1 16`, collage depuis le presse-papiers
- Octets contrôlés (0-255), masques **contigus uniquement**
- Clavier intégré (chiffres, `.`, `/`, Coller, Mon IP) avec point automatique après un octet complet
- Curseur de préfixe à **échelle non linéaire** : l'octet d (/24 → /32) occupe ~45 % de la course, retour haptique à chaque cran
- Réseau, première/dernière adresse, broadcast, hôtes, masque, wildcard, zone DNS inverse : un appui copie la valeur
- Nature de l'adresse : publique, privée RFC 1918, CGNAT RFC 6598, loopback, lien local, multicast (et ses portées), documentation, réservée, broadcast…
- Vue binaire repliable (bits réseau / hôtes)
- Historique compact (feuille du bas)

**Découpage**
- En N parts égales ou par besoins (VLSM), barre d'occupation et blocs libres restants
- Copie du plan d'adressage en texte

**IPv6**
- Formes compressée (RFC 5952) et développée, IPv4 embarquée, `[crochets]`, `%zone`
- Réseau, première/dernière adresse, nombre d'adresses, nombre de /64, zone `ip6.arpa`
- Nature : unicast global, ULA, lien local, multicast et portée, documentation, NAT64, 6to4, Teredo…

## Installer

Télécharge le dernier `subnet-x.y.z.apk` dans les [Releases](../../releases) et ouvre-le sur le téléphone
(autoriser l'installation depuis cette source si Android le demande).

## Compiler

GitHub Actions compile et teste à chaque push sur `main`, puis publie (ou met à jour) la release `v` + contenu du fichier `VERSION`. Pour sortir une nouvelle version, change `VERSION`.
En local : `./gradlew assembleRelease` (JDK 17 et SDK Android requis).

L'APK est signé avec la clé `app/subnet-release.jks` versionnée dans le dépôt, pour que chaque build
puisse mettre à jour l'app installée. C'est suffisant pour un usage personnel ; pour une diffusion publique,
remplace-la par une clé gardée en secret.

## Structure

- `app/src/main/java/fr/globoudou/subnet/core` : calculs purs (testés dans `app/src/test`)
- `app/src/main/java/fr/globoudou/subnet/ui` : écrans Compose
- `app/src/main/java/fr/globoudou/subnet/data` : historique, réglages, adresse locale

# Numeri

Jogo de puzzle baseado no Slitherlink, para Android. É a versão Kotlin do Numeri original para iPhone (2009).

Toca numa casa, ou arrasta o dedo por várias, para trocar os seus lados. O objetivo é fechar uma única linha
que deixe todos os números certos: cada número diz quantos lados da sua casa fazem parte da linha.
O botão `>` mostra a solução ou passa a um puzzle novo; o `<` limpa o tabuleiro. Virar o telemóvel para baixo
revela os números escondidos.

## Compilar

Precisa do Android SDK com a plataforma 37 (indicado em `local.properties` com `sdk.dir=...`) e do JDK 21.

```
./gradlew assembleDebug            # APK em app/build/outputs/apk/debug/
./gradlew testDebugUnitTest        # testes da lógica do jogo
./gradlew recordPaparazziDebug     # imagens dos ecrãs em app/src/test/snapshots/images/
```

## Versões

Criar e enviar uma etiqueta `vX.Y` (igual ao `versionName` em `app/build.gradle.kts`) corre a ação
`.github/workflows/release.yml`, que testa, compila o APK de release assinado e o publica nas versões do GitHub.
A chave de assinatura está nos segredos do repositório `NUMERI_KEYSTORE_BASE64` e `NUMERI_KEYSTORE_PASSWORD`.

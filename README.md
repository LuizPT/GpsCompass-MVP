# GPS Compass ZQ9109 — MVP

Protótipo Android leve, com um tipo de widget redimensionável para Nova Launcher e rádios sem magnetómetro. A direção apresentada é a direção de deslocação comunicada pelo GPS; pode diferir da orientação física do veículo, por exemplo ao fazer marcha-atrás.

## Origem e distribuição

Este MVP adapta [ATOTO_GpsCompass](https://github.com/tyguy101a/ATOTO_GpsCompass) e é publicado com autorização de redistribuição do titular dos direitos da origem. O repositório original não identifica uma licença pública; esta publicação não atribui uma licença open source ao código de origem.

## Comportamento

- Usa `LocationManager` e apenas `GPS_PROVIDER`; não lê magnetómetro nem serviços de localização em rede.
- A pequena agulha vetorial roda com o rumo GPS válido, quando a velocidade é de pelo menos 1 m/s. A imagem é redesenhada junto das correções GPS, no máximo a cada três segundos; não há animação contínua nem trabalho gráfico em segundo plano. Sem rumo válido, a última direção fica esbatida e identificada como “Último rumo”.
- Mostra latitude e longitude WGS84 com seis casas decimais, precisão horizontal de `Location.getAccuracy()` e hora da correção.
- Depois de 15 segundos sem correção GPS recente, marca a posição como antiga. O serviço mantém uma única subscrição GPS.
- Depois da permissão, toque no widget para iniciar o serviço de localização. O último widget removido ou a ação “Parar GPS” na notificação encerra o serviço.
- Os botões 112 e 117 abrem confirmação e usam `ACTION_DIAL`; não iniciam chamadas.
- Não inclui integração ATOTO/SYU, vigilância de câmara, reinícios automáticos, anúncios, telemetria, contas ou dependências de rede.

## Compilar e instalar

Com Android SDK 34 e Java 17:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Permissões declaradas: localização precisa e aproximada (a aplicação exige precisa para rumo GPS), serviço em primeiro plano de localização e notificações (para a notificação contínua com ação para parar). Não é pedida localização em segundo plano.

O emulador pode validar interface, permissões e caminhos sem GPS simulado. A qualidade e disponibilidade de rumo dependem do recetor GPS e devem ser verificadas no ZQ9109 em movimento.

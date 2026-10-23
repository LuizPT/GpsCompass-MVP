# Alterações do MVP 1.8

- Serviço GPS retoma automaticamente após reinício do rádio ou atualização da aplicação, com ativação persistente e respeito pela ação “Parar GPS”. Em Android 10+, localização “sempre” é solicitada separadamente para o arranque automático.

- Publicação pública no GitHub com autorização de redistribuição; atualização do rumo visual através de agulha vetorial leve.
- Widget horizontal redimensionável com direção GPS, coordenadas, precisão e hora.
- Atualização do rumo apenas com direção válida e velocidade GPS mínima de 1 m/s; rumo conservado fica identificado como último.
- Estado separa GPS indisponível, falta de sinal e coordenadas antigas.
- Botões de emergência com confirmação e `ACTION_DIAL`; botão de cópia de coordenadas.
- Removidos serviços duplicados, watchdogs, reinícios agressivos e integrações ATOTO/SYU/câmara.

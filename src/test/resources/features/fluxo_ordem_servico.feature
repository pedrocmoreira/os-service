# language: pt
Funcionalidade: Fluxo completo da Ordem de Serviço coordenado pela Saga orquestrada
  Como oficina mecânica com múltiplas filiais
  Quero que a OS percorra diagnóstico, orçamento, aprovação, pagamento e execução
  Para que cada etapa distribuída seja consistente e falhas sejam compensadas

  Contexto:
    Dado que existe o cliente "João Silva" com o veículo de placa "ABC1D23"
    E o catálogo possui o serviço "Troca de óleo" por 150.00 e a peça "Óleo 5W30" por 38.00

  Cenário: Caminho feliz - da abertura até a entrega do veículo
    Quando a atendente abre uma OS com o serviço "Troca de óleo" e 4 unidades da peça "Óleo 5W30"
    Então a OS deve estar com status "RECEBIDA" e valor total 302.00
    E o comando "command.execution.diagnosis.start" deve ter sido publicado
    Quando o evento "event.execution.diagnosis.started" for recebido
    Então a OS deve estar com status "EM_DIAGNOSTICO"
    Quando o evento "event.execution.diagnosis.completed" for recebido
    Então o comando "command.billing.budget.create" deve ter sido publicado
    Quando o evento "event.billing.budget.created" for recebido
    Então a OS deve estar com status "AGUARDANDO_APROVACAO"
    Quando o evento "event.billing.budget.approved" for recebido
    Então a OS deve estar com status "AGUARDANDO_PAGAMENTO"
    Quando o evento "event.billing.payment.confirmed" for recebido
    Então o comando "command.execution.parts.reserve" deve ter sido publicado
    Quando o evento "event.execution.parts.reserved" for recebido
    Então a OS deve estar com status "EM_EXECUCAO"
    Quando o evento "event.execution.completed" for recebido
    Então a OS deve estar com status "FINALIZADA"
    E a saga deve estar "COMPLETED"
    Quando a atendente registra a entrega do veículo
    Então a OS deve estar com status "ENTREGUE"
    E o histórico da OS deve conter 7 registros

  Cenário: Cliente recusa o orçamento - compensação cancela a execução e o orçamento
    Dado que uma OS foi aberta e o orçamento já foi enviado ao cliente
    Quando o evento "event.billing.budget.rejected" for recebido
    Então a OS deve estar com status "CANCELADA"
    E os comandos de compensação "command.execution.cancel,command.billing.budget.cancel" devem ter sido publicados
    E a saga deve estar "COMPENSATED"

  Cenário: Pagamento recusado pelo Mercado Pago
    Dado que uma OS foi aberta e o orçamento já foi enviado ao cliente
    Quando o evento "event.billing.budget.approved" for recebido
    E o evento "event.billing.payment.failed" for recebido com motivo "cc_rejected_insufficient_amount"
    Então a OS deve estar com status "CANCELADA"
    E o motivo do cancelamento deve conter "cc_rejected_insufficient_amount"
    E a saga deve estar "COMPENSATED"

  Cenário: Falta de peças após o pagamento - estorno do pagamento
    Dado que uma OS foi aberta e o orçamento já foi enviado ao cliente
    Quando o evento "event.billing.budget.approved" for recebido
    E o evento "event.billing.payment.confirmed" for recebido
    E o evento "event.execution.parts.reservation-failed" for recebido com motivo "Estoque insuficiente para Óleo 5W30"
    Então a OS deve estar com status "CANCELADA"
    E os comandos de compensação "command.execution.cancel,command.billing.payment.refund" devem ter sido publicados
    E a saga deve estar "COMPENSATING"
    Quando o evento "event.billing.payment.refunded" for recebido
    Então a saga deve estar "COMPENSATED"

  Cenário: Mensagem duplicada não é processada duas vezes
    Dado que uma OS foi aberta e o orçamento já foi enviado ao cliente
    Quando o mesmo evento "event.billing.budget.approved" for recebido duas vezes
    Então a OS deve estar com status "AGUARDANDO_PAGAMENTO"
    E o histórico da OS deve conter 4 registros

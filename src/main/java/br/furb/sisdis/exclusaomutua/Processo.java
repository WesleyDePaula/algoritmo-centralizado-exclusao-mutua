package br.furb.sisdis.exclusaomutua;

import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper=true)
public class Processo extends Thread {

	public static final String LOG_SEPARATOR = "--------------------------------------------------------------------------------------------------------";
	private static Random RANDOM = new Random();

	private String processoId;

	private Queue<Processo> filaProcessosConsumo = new LinkedBlockingQueue<>();

	private Processo processoCoordenador;

	private boolean recursoEmUso = false;

	private StatusProcesso status = StatusProcesso.IDLE;

	private ExecutorService executor;

	public Processo(String processoId) {
		super();
		this.processoId = processoId;
	}

	@Override
	public void run() {
        /**
         * While responsavel por executar rotinas referentes a processos coordenados, realizando uma pausa entre 10 - 25 segundos
         * após a pausa, realiza uma chamada ao coordenador requisitando o uso do recurso
         */
		while (!this.isInterrupted() && this.status != StatusProcesso.COORDENADOR) {
			try {
				Thread.sleep(RANDOM.nextLong(10000, 25001));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}

			if (status == StatusProcesso.IDLE) {
				requisitaRecurso();
			}
		}

        /**
         * Caso o processo se torne/seja coordenador, executa while responsável pela rotina do mesmo
         */
        if (this.status == StatusProcesso.COORDENADOR) {
            iniciarCoordenacao();
        }
	}

    /**
     * Método responsavel por iniciar a rotina de um coordenador.
     *
     */
	private void iniciarCoordenacao() {
		log.info("# Processo {} atuando como coordenador, consumindo processos da fila", processoId);
		this.executor = Executors.newSingleThreadExecutor();

		while (this.status == StatusProcesso.COORDENADOR && !this.isInterrupted()) {
			processarFilaConsumo();

			// Pequena pausa para evitar busy waiting
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

    /**
     * chamada para processar a fila dos processos que requisitaram consumo do recurso
     * apenas libera o recurso se não estiver em uso
     */
	private void processarFilaConsumo() {
		Processo processo = filaProcessosConsumo.peek();
		if (processo != null && !recursoEmUso) {
			filaProcessosConsumo.remove(processo);

			if (processo.isAlive() && !Objects.equals(processo, this)) {
				log.info(LOG_SEPARATOR);
				log.info("# Coordenador {} concede permissão para o processo {} consumir o recurso", processoId, processo.getProcessoId());

				recursoEmUso = true;
				processo.setStatus(StatusProcesso.CONSUMINDO_RECURSO);

				executor.submit(processo::consumirRecurso);
			}
		}
	}

	/**
	 * Método responsável pelo consumo do recurso por parte do processo
	 */
	private void consumirRecurso() {
		log.info("## Processo {} consumindo recurso...", processoId);
		Recurso.consumir();

        /**
         * Verifica se o coordenador ainda está ativo antes de liberar
         * Processo realiza chamada ao coordenador, avisando que pode liberar
          */

		if (processoCoordenador != null && processoCoordenador.getStatus() == StatusProcesso.COORDENADOR) {
			processoCoordenador.liberarRecurso(this);
		}
	}

    /**
     * Método responsável pela liberação do recurso por parte do coordenador
     * @param processoASerLiberado
     */
	private void liberarRecurso(Processo processoASerLiberado) {
		log.info(LOG_SEPARATOR);
		if (this.status != StatusProcesso.COORDENADOR) {
			return;
		}

		processoASerLiberado.setStatus(StatusProcesso.IDLE);
		recursoEmUso = false;

		log.info("#### Coordenador {} liberou o recurso do processo {}", processoId, processoASerLiberado.getProcessoId());
	}

    /**
     * Metodo por parte do processo que solicita o recurso ao coordenador
     */
	private void requisitaRecurso() {
		if (Objects.equals(this, processoCoordenador)) {
			return;
		}

		log.info(LOG_SEPARATOR);
		log.info("# Processo {} solicitando recurso ao coordenador", processoId);

		if (processoCoordenador != null && processoCoordenador.getStatus() == StatusProcesso.COORDENADOR) {
			processoCoordenador.adicionaProcessoAFilaConsumo(this);
		}
	}

    /**
     * Metodo por parte do coordenador que adiciona o processo a fila de consumo, para controle,
     * seta o status do processo como AGUARDANDO_RECURSO
     * @param processo
     */
	private void adicionaProcessoAFilaConsumo(Processo processo) {
		if (this.status != StatusProcesso.COORDENADOR || Objects.equals(processo, this)) {
			return;
		}

		if (filaProcessosConsumo.contains(processo)) {
			log.info("## Processo {} já está na fila, requisição ignorada \n ## Processos na fila de consumo: {}", processo.getProcessoId(), filaProcessosConsumo.stream().map(Processo::getProcessoId).toList());
			return;
		}

		processo.setStatus(StatusProcesso.AGUARDANDO_RECURSO);
		filaProcessosConsumo.add(processo);
		log.info("## Coordenador {} adicionou processo {} a fila de consumo", processoId, processo.getProcessoId());
		log.info("## Processos na fila de consumo: {}", filaProcessosConsumo.stream().map(Processo::getProcessoId).toList());
	}

    /**
     * Responsável por interromper coordenador e limpar a fila de processos
     */
	public synchronized void eliminarProcesso() {
		if (executor != null) {
			executor.shutdownNow();
		}
		this.interrupt();
		this.filaProcessosConsumo.clear();
	}
}

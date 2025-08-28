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

	public Processo(String processoId) {
		super();
		this.processoId = processoId;
	}

	@Override
	public void run() {
		while (!Objects.equals(this, processoCoordenador)) {
			try {
				Thread.sleep(RANDOM.nextLong(10000, 25001));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			requisitaRecurso();
		}

		log.info("# Processo {} atuando como coordenador, consumindo processos da fila", processoId);
		while (!this.isInterrupted()) {
			Processo processo = filaProcessosConsumo.peek();
			if (Objects.nonNull(processo) && !recursoEmUso) {
				log.info(LOG_SEPARATOR);
				log.info("# Coordenador {} concede permissão para o processo {} consumir o recurso", processoId, processo.getProcessoId());

				recursoEmUso = true;
				ExecutorService executor = Executors.newSingleThreadExecutor();
				executor.submit(processo::consumirRecurso);
			}

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
	 * Método que simula o consumo do recurso
	 */
	private void consumirRecurso() {
		log.info(LOG_SEPARATOR);
		log.info("## Processo {} consumindo recurso...", processoId);
		Recurso.consumir();
		processoCoordenador.liberarRecurso(this);
	}

	private void liberarRecurso(Processo processoASerLiberado) {
		if (this.filaProcessosConsumo.remove(processoASerLiberado)) {
			recursoEmUso = false;
			log.info("#### Coordenador {} liberou o recurso do processo {}", processoId, processoASerLiberado.getProcessoId());
		} else {
			log.warn("#### Coordenador {} não encontrou o processo {} na fila de consumo", processoId, processoASerLiberado.getProcessoId());
		}
	}

	private void requisitaRecurso() {
		log.info(LOG_SEPARATOR);
		log.info("# Processo {} solicitando recurso ao coordenador", processoId);
		processoCoordenador.adicionaProcessoAFilaConsumo(this);
	}

	private void adicionaProcessoAFilaConsumo(Processo processo) {
		if (filaProcessosConsumo.contains(processo)) {
			log.info("## Processo {} já está na fila, requisição ignorada \n ## Processos na fila de consumo: {}", processo.getProcessoId(), filaProcessosConsumo.stream().map(Processo::getProcessoId).toList());
			return;
		}

		filaProcessosConsumo.add(processo);
		log.info("## Coordenador {} adicionou processo {} a fila de consumo \n ## Processos na fila de consumo: {}", processoId, processo.getProcessoId(), filaProcessosConsumo.stream().map(Processo::getProcessoId).toList());
	}

	public void eliminarProcesso() {
		this.interrupt();
		this.filaProcessosConsumo.clear();
	}
}

package br.furb.sisdis.exclusaomutua;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
	
	private static List<Processo> processos = new LinkedList<>();
	
	private static Processo coordenador;

	private static final Random RANDOM = new Random(); 
	
	public static void main(String[] args) {
		log.info("--- Aplicação inicializada ---");
		
		for (int i = 0; i < 4; i++) {
			criaProcesso();
		}
		elegerNovoCoordenador();
		log.info("--------------------------------------------------------------------------------------------------------");
		
		// Cria um executor de threads
		// TODO: VERIFICAR SE A EXECUÇÃO DE THREADS PARA CRIAR PROCESSOS E ELIMINAR COORDENADORES DEVEM SER FEITAS EM SEQUENCIA, SEM UMA SOBREPOR A OUTRA
		ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(); //Executors.newScheduledThreadPool(2); -> Prepara o pool em duas threads
		
		
		Runnable criaProcessos = () -> criaProcesso();
		Runnable eliminaCoordenadorAtual = () -> eliminarCoordenador();
		
		scheduledExecutor.scheduleAtFixedRate(criaProcessos, 40, 60, TimeUnit.SECONDS);
		scheduledExecutor.scheduleAtFixedRate(eliminaCoordenadorAtual, 60, 60, TimeUnit.SECONDS);
		
	}

	/**
	 * Define método que elimina o coordenador atual
	 *
	 */
	private static void eliminarCoordenador() {
			if (coordenador != null) {
				log.info("--------------------------------------------------------------------------------------------------------");
				log.info("# Iniciado processo de eliminação do coordenador {}", coordenador.getProcessoId());
				
				//TODO: Deve remover também a lista de consumo dos processos
				coordenador.interrupt();
				processos.remove(coordenador);
				
				log.info("## Coordenador {} eliminado com sucesso | Total de processos ativos: {}", coordenador.getProcessoId(), processos.size());
				coordenador = null;
			}
		
		
		// TODO: Realizar a eleição aleatória
		elegerNovoCoordenador();
		log.info("--------------------------------------------------------------------------------------------------------");
	}

	
	/**
	 * Realiza a eleição aleatória de processo coordenador
	 */
	private static void elegerNovoCoordenador() {
		log.info("### Iniciado processo de eleição de novo processo coordenador");
		
		if (processos.isEmpty()) {
			log.info("### Falha ao eleger um novo coordenador: não há processos ativos");
			return;
		}
		
		var ultimoIndice = RANDOM.nextInt(0, processos.size() - 1);
		coordenador = processos.get(ultimoIndice);
		log.info("#### Eleito novo coordenador: {}", coordenador.getProcessoId());
		
	}

	/**
	 * Realiza a criação de processos e adiciona na lista de processos ativos
	 */
	private static void criaProcesso() {
		var processo = new Processo();
		processo.start();
		processos.add(processo);
		log.info("# Novo Processo {} criado | Total de processos ativos: {}", processo.getProcessoId(), processos.size());
	}
	
}

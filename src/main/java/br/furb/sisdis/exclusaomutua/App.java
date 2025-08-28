package br.furb.sisdis.exclusaomutua;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

	public static final String LOG_SEPARATOR = "--------------------------------------------------------------------------------------------------------";
	private static List<Processo> processos = new LinkedList<>();
	
	private static Processo coordenador;

	private static final Random RANDOM = new Random(); 

	private static int sequencialProcesso = 1;

	public static void main(String[] args) {
		log.info("### Aplicação inicializada ###");

		// Cria 4 processos iniciais e define o coordenador
		for (int i = 0; i < 4; i++) {
			criaProcesso(false);
		}
		elegerNovoCoordenador();

		// Inicia todos os processos
		processos.forEach(Processo::start);
		log.info(LOG_SEPARATOR);
		
		// Cria um executor de threads que permanece ativo
        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

        Runnable criaProcessos = () -> criaProcesso(true);
        Runnable eliminaCoordenadorAtual = App::eliminarCoordenador;

        scheduledExecutor.scheduleAtFixedRate(criaProcessos, 40, 40, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(eliminaCoordenadorAtual, 60, 60, TimeUnit.SECONDS);

    }

	/**
	 * Define método que elimina o coordenador atual
	 *
	 */
	private static void eliminarCoordenador() {
			if (coordenador != null) {
				log.info(LOG_SEPARATOR);
				log.info("# Iniciado processo de eliminação do coordenador {}", coordenador.getProcessoId());
				
				coordenador.eliminarProcesso();
				processos.remove(coordenador);
				
				log.info("## Coordenador {} eliminado com sucesso | Total de processos ativos: {}", coordenador.getProcessoId(), processos.size());
				coordenador = null;
			}

		elegerNovoCoordenador();
		log.info(LOG_SEPARATOR);
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
		
		var ultimoIndice = RANDOM.nextInt(0, processos.size());
		coordenador = processos.get(ultimoIndice);

		// Seta o coordenador para todos os outros processos
		processos.forEach(processo -> processo.setProcessoCoordenador(coordenador));

		log.info("#### Eleito novo coordenador: {}", coordenador.getProcessoId());
	}

	/**
	 * Realiza a criação de processos e adiciona na lista de processos ativos
	 */
	private static void criaProcesso(boolean startProcesso) {
		var processo = new Processo("P" + sequencialProcesso++);
		processos.add(processo);
		processo.setProcessoCoordenador(coordenador);

		if (startProcesso) {
			processo.start();
		}
		log.info(LOG_SEPARATOR);
		log.info("# Novo Processo {} criado | Total de processos ativos: {}", processo.getProcessoId(), processos.size());
	}
	
}

package br.furb.sisdis.exclusaomutua;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@EqualsAndHashCode(callSuper=true)
public class Processo extends Thread {
	
	private UUID processoId;
	
	private Queue<Processo> listaProcessosConsumo = new LinkedBlockingQueue<Processo>();
	
	public Processo() {
		super();
		this.processoId = UUID.randomUUID();
	}
	
}

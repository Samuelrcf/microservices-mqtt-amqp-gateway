package com.ufersa.apigateway.services.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Service;

import com.ufersa.apigateway.constants.GlobalConstants;

import jakarta.annotation.PostConstruct;

@Service
public class BancoDeDados {

	private final int porta = GlobalConstants.PORTA_BD;
	private File arquivoSaida;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final BlockingQueue<String> buffer = new LinkedBlockingQueue<>();
	private final int BUFFER_FLUSH_INTERVAL_MS = 5000; // flush a cada 5 segundos
	private Thread persistenciaThread;
	private volatile boolean executando = true;

	@PostConstruct
	public void iniciarConexao() {
	    this.arquivoSaida = new File(GlobalConstants.BD_TXT);
	    persistenciaThread = new Thread(this::persistirBuffer);
	    persistenciaThread.start();

	    // Inicie o socket em uma thread separada para não bloquear a inicialização do Spring
	    new Thread(() -> {
	        try (ServerSocket serverSocket = new ServerSocket(porta)) {
	            System.out.println("Banco de dados escutando na porta " + porta);

	            while (executando) {
	                Socket socket = serverSocket.accept();
	                executor.submit(() -> lidarComConexao(socket));
	            }

	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	            executando = false;
	            persistenciaThread.interrupt();
	            executor.shutdown();
	        }
	    }).start();
	}

	private void lidarComConexao(Socket socket) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
			String linhaRecebida = in.readLine();

			if (linhaRecebida != null && !linhaRecebida.isBlank()) {
				salvarDado(linhaRecebida);
				out.println("Dado recebido e armazenado com sucesso.");
			}

		} catch (IOException e) {
			System.out.println("Erro ao processar conexão: " + e.getMessage());
		}
	}

	private void salvarDado(String linha) {
		buffer.offer(linha);
	}

	private void persistirBuffer() {
		while (executando) {
			try {
				Thread.sleep(BUFFER_FLUSH_INTERVAL_MS);
				flushBufferParaArquivo();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private synchronized void flushBufferParaArquivo() {
		try (FileWriter writer = new FileWriter(arquivoSaida, true); BufferedWriter bw = new BufferedWriter(writer)) {
			String dado;
			while ((dado = buffer.poll()) != null) {
				bw.write(dado);
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println("Erro ao salvar dados do buffer: " + e.getMessage());
		}
	}

}

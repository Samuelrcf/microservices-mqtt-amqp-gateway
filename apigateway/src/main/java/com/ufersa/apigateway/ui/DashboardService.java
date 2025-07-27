package com.ufersa.apigateway.ui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ufersa.apigateway.constants.GlobalConstants;

@Service
public class DashboardService {

    private static final String ARQUIVO_DADOS = GlobalConstants.BD_TXT;

    public Map<String, Object> gerarRelatorio() {
        Map<String, Double> somaTemperatura = new HashMap<>();
        Map<String, Double> somaUmidade = new HashMap<>();
        Map<String, Double> somaPressao = new HashMap<>();
        Map<String, Double> somaRadiacao = new HashMap<>();

        Map<String, Integer> totalTemperatura = new HashMap<>();
        Map<String, Integer> totalUmidade = new HashMap<>();
        Map<String, Integer> totalPressao = new HashMap<>();
        Map<String, Integer> totalRadiacao = new HashMap<>();

        int totalDados = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(ARQUIVO_DADOS))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.isBlank()) continue;

                String[] partes = linha.split("\\] \\["); 
                String regiao = partes[0].replace("[", "").trim();
                String[] valores = partes[1].replace("]", "").split("\\|");

                double temperatura = Double.parseDouble(valores[0].trim().replace(",", "."));
                double umidade = Double.parseDouble(valores[1].trim().replace(",", "."));
                double pressao = Double.parseDouble(valores[2].trim().replace(",", "."));
                double radiacao = Double.parseDouble(valores[3].trim().replace(",", "."));

                totalDados += 4;

                atualizarMetricas(somaTemperatura, totalTemperatura, regiao, temperatura);
                atualizarMetricas(somaUmidade, totalUmidade, regiao, umidade);
                atualizarMetricas(somaPressao, totalPressao, regiao, pressao);
                atualizarMetricas(somaRadiacao, totalRadiacao, regiao, radiacao);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo de dados", e);
        }

        Map<String, Double> mediaTemperatura = calcularMedias(somaTemperatura, totalTemperatura);
        Map<String, Double> mediaUmidade = calcularMedias(somaUmidade, totalUmidade);
        Map<String, Double> mediaPressao = calcularMedias(somaPressao, totalPressao);
        Map<String, Double> mediaRadiacao = calcularMedias(somaRadiacao, totalRadiacao);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("totalDados", totalDados);
        resultado.put("mediaTemperatura", mediaTemperatura);
        resultado.put("mediaUmidade", mediaUmidade);
        resultado.put("mediaPressao", mediaPressao);
        resultado.put("mediaRadiacao", mediaRadiacao);

        resultado.put("percentualTemperatura", calcularPercentuais(mediaTemperatura));
        resultado.put("percentualUmidade", calcularPercentuais(mediaUmidade));
        resultado.put("percentualPressao", calcularPercentuais(mediaPressao));
        resultado.put("percentualRadiacao", calcularPercentuais(mediaRadiacao));

        return resultado;
    }

    private void atualizarMetricas(Map<String, Double> soma, Map<String, Integer> total, String regiao, double valor) {
        soma.put(regiao, soma.getOrDefault(regiao, 0.0) + valor);
        total.put(regiao, total.getOrDefault(regiao, 0) + 1);
    }

    private Map<String, Double> calcularMedias(Map<String, Double> soma, Map<String, Integer> total) {
        Map<String, Double> medias = new HashMap<>();
        for (String regiao : soma.keySet()) {
            int count = total.getOrDefault(regiao, 0);
            if (count > 0) {
                medias.put(regiao, soma.get(regiao) / count);
            }
        }
        return medias;
    }

    private Map<String, Double> calcularPercentuais(Map<String, Double> medias) {
        Map<String, Double> percentuais = new HashMap<>();
        double somaMediasAbs = medias.values().stream().mapToDouble(Math::abs).sum();

        if (somaMediasAbs == 0.0) return percentuais;

        for (Map.Entry<String, Double> entry : medias.entrySet()) {
            double percentual = (Math.abs(entry.getValue()) / somaMediasAbs) * 100.0;
            percentuais.put(entry.getKey(), percentual);
        }

        return percentuais;
    }
}




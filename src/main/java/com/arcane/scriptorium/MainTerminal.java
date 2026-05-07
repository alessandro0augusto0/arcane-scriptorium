package com.arcane.scriptorium;

public class MainTerminal {
    private static final long DURACAO_PADRAO_MS = 15000L;

    public static void main(String[] args) {
        BibliotecaController controller = new BibliotecaController();
        controller.adicionarLeitor(1, "Mago Leitor");
        controller.adicionarLeitor(2, "Maga Leitura");
        controller.adicionarLeitor(3, "Leitor Arcano");
        controller.adicionarLeitorCritico(4, "Mago Critico");
        controller.adicionarEscritor(5, "Ritualista");
        controller.adicionarEscritor(6, "Escriba Supremo");

        controller.setLogListener(System.out::println);
        controller.iniciarSimulacao();

        long duracaoMs = parseDuracao(args);
        dormir(duracaoMs);

        String relatorio = controller.pararSimulacao();
        System.out.println(relatorio);
    }

    private static long parseDuracao(String[] args) {
        if (args.length == 0) {
            return DURACAO_PADRAO_MS;
        }
        try {
            return Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            return DURACAO_PADRAO_MS;
        }
    }

    private static void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

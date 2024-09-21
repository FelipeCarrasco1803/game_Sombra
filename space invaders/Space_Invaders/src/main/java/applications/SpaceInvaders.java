package applications;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpaceInvaders extends JFrame implements ActionListener, KeyListener {
    private Timer cronometro;
    private NaveJogador jogador;
    private List<Inimigo> inimigos;
    private List<Tiro> tiros;
    private List<TiroChefe> tirosChefe;
    private Chefe sombra;
    private boolean jogoRodando = true;
    private int pontuacao = 0;
    private int nivel = 1;
    private long tempoUltimoTiro;
    private Random random;
    private boolean[] teclas = new boolean[256];

    // Variáveis para velocidade e frequência do tiro do chefe
    private final int velocidadeTiroChefe = 150;
    private final long frequenciaTiroChefe = 500; // em milissegundos

    public SpaceInvaders() {
        setTitle("Invasores Espaciais");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        jogador = new NaveJogador();
        inimigos = new ArrayList<>();
        tiros = new ArrayList<>();
        tirosChefe = new ArrayList<>();
        random = new Random();

        inicializarInimigos();

        cronometro = new Timer(1000 / 60, this);
        cronometro.start();

        addKeyListener(this);
        setFocusable(true);
        setVisible(true);
    }

    private void inicializarInimigos() {
        inimigos.clear();
        for (int linha = 0; linha < 3; linha++) {
            for (int coluna = 0; coluna < 5; coluna++) {
                inimigos.add(new Inimigo(coluna * 50 + 50, linha * 50 + 50, 2, randomColor()));
            }
        }
    }

    private Color randomColor() {
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (jogoRodando) {
            desenhar(g);
        } else {
            mostrarTelaGameOver(g);
        }
    }

    private void desenhar(Graphics g) {
        jogador.draw(g);
        for (Inimigo inimigo : inimigos) {
            inimigo.draw(g);
        }
        for (Tiro tiro : tiros) {
            tiro.draw(g);
        }
        for (TiroChefe tiro : tirosChefe) {
            tiro.draw(g);
        }
        if (sombra != null) {
            sombra.draw(g);
        }
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Pontuação: " + pontuacao, 10, 20);
        g.drawString("Nível: " + nivel, 700, 20);
    }

    private void mostrarTelaGameOver(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("Game Over, Sombra dominou a Terra", 320, 290);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Pontuação: " + pontuacao, 370, 320);
    }

    private void atualizarJogo() {
        if (!jogoRodando) return;

        if (teclas[KeyEvent.VK_LEFT]) {
            jogador.moverEsquerda();
        }
        if (teclas[KeyEvent.VK_RIGHT]) {
            jogador.moverDireita();
        }
        if (teclas[KeyEvent.VK_SPACE]) {
            dispararTiro();
        }

        jogador.atualizar();
        atualizarTiros();
        atualizarInimigos();
        verificarColisoes();

        if (inimigos.isEmpty()) {
            nivel++;
            if (nivel > 10) {
                if (sombra == null) {
                    sombra = new Chefe(370, 50);
                }
                if (sombra.isDerrotado()) {
                    mostrarTelaEncerramento();
                }
            } else {
                inicializarInimigos();
            }
        }

        if (sombra != null) {
            sombra.atualizar(jogador, velocidadeTiroChefe, frequenciaTiroChefe);
            tirosChefe.addAll(sombra.getTiros());
        }

        repaint();
    }

    private void atualizarTiros() {
        List<Tiro> tirosParaRemover = new ArrayList<>();
        for (Tiro tiro : tiros) {
            tiro.atualizar();
            if (tiro.y < 0) {
                tirosParaRemover.add(tiro);
            }
        }
        tiros.removeAll(tirosParaRemover);

        List<TiroChefe> tirosChefeParaRemover = new ArrayList<>();
        for (TiroChefe tiro : tirosChefe) {
            tiro.atualizar();
            if (tiro.y > 600) {
                tirosChefeParaRemover.add(tiro);
            }
        }
        tirosChefe.removeAll(tirosChefeParaRemover);
    }

    private void atualizarInimigos() {
        for (Inimigo inimigo : inimigos) {
            inimigo.atualizar();
            if (inimigo.y > 580) {
                jogoRodando = false;
                JOptionPane.showMessageDialog(this, "Game Over! PERDEU MANÉ.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        }
    }

    private void verificarColisoes() {
        List<Tiro> tirosParaRemover = new ArrayList<>();
        List<Inimigo> inimigosParaRemover = new ArrayList<>();

        for (Tiro tiro : tiros) {
            Iterator<Inimigo> inimigoIterator = inimigos.iterator();
            while (inimigoIterator.hasNext()) {
                Inimigo inimigo = inimigoIterator.next();
                if (tiro.colideCom(inimigo)) {
                    inimigoIterator.remove();
                    tirosParaRemover.add(tiro);
                    pontuacao += 10;
                    aumentarVelocidadeInimigos();
                    break;
                }
            }
        }

        if (sombra != null) {
            for (Tiro tiro : tiros) {
                if (tiro.colideCom(sombra)) {
                    sombra.receberDano();
                    tirosParaRemover.add(tiro);
                    if (sombra.isDerrotado()) {
                        mostrarTelaEncerramento();
                    }
                }
            }
        }

        for (TiroChefe tiro : tirosChefe) {
            if (tiro.colideCom(jogador)) {
                jogoRodando = false;
                JOptionPane.showMessageDialog(this, "Game Over! Você foi atingido pelo chefe!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        }

        tiros.removeAll(tirosParaRemover);
    }

    private void aumentarVelocidadeInimigos() {
        for (Inimigo inimigo : inimigos) {
            inimigo.aumentarVelocidade();
        }
    }

    private void mostrarTelaEncerramento() {
        jogoRodando = false;
        JOptionPane.showMessageDialog(this,
                "Parabéns! Você derrotou o Chefe Sombra!\n" +
                        "A Terra está segura mais uma vez!\n" +
                        "Pontuação final: " + pontuacao,
                "Fim do Jogo", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void dispararTiro() {
        long tempoAtual = System.currentTimeMillis();
        if (tempoAtual - tempoUltimoTiro >= 200) {
            tiros.add(new Tiro(jogador.x + 25, 560));
            tempoUltimoTiro = tempoAtual;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        atualizarJogo();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        teclas[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        teclas[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpaceInvaders::new);
    }

    // Classes internas
    class NaveJogador {
        public int x = 375;
        private static final int DISTANCIA_MOVIMENTO = 10;

        public void moverEsquerda() {
            if (x > 0) x -= DISTANCIA_MOVIMENTO;
        }

        public void moverDireita() {
            if (x < 750) x += DISTANCIA_MOVIMENTO;
        }

        public void draw(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillRect(x, 580, 50, 20);
        }

        public void atualizar() {}
    }

    class Inimigo {
        int x;
        public int y;
        double velocidade = 15;
        Color cor;

        public Inimigo(int x, int y, int velocidadeInicial, Color cor) {
            this.x = x;
            this.y = y;
            this.velocidade = velocidadeInicial;
            this.cor = cor;
        }

        public void atualizar() {
            x += velocidade;
            if (x < 0 || x > 760) {
                velocidade = -velocidade;
                y += 20;
            }
        }

        public void draw(Graphics g) {
            g.setColor(cor);
            g.fillRect(x, y, 40, 20);
        }

        public void aumentarVelocidade() {
            velocidade *= 1.2;
        }
    }

    class Tiro {
        public int x;
        public int y;
        private final int velocidade = 15;

        public Tiro(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void atualizar() {
            y -= velocidade;
        }

        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, 5, 15);
        }

        public boolean colideCom(Inimigo inimigo) {
            return x < inimigo.x + 40 && x + 5 > inimigo.x && y < inimigo.y + 20 && y + 15 > inimigo.y;
        }

        public boolean colideCom(Chefe chefe) {
            return x < chefe.x + 120 && x + 5 > chefe.x && y < chefe.y + 60 && y + 15 > chefe.y;
        }
    }

    class TiroChefe {
        public int x;
        public int y;
        private final int velocidade;

        public TiroChefe(int x, int y, int velocidade) {
            this.x = x;
            this.y = y;
            this.velocidade = velocidade;
        }

        public void atualizar() {
            y += velocidade; // Move para baixo
        }

        public void draw(Graphics g) {
            g.setColor(Color.ORANGE);
            g.fillRect(x, y, 5, 15);
        }

        public boolean colideCom(NaveJogador jogador) {
            return x < jogador.x + 50 && x + 5 > jogador.x && y + 15 >= 580;
        }
    }

    class Chefe {
        public int x;
        public int y;
        private int vida = 10;
        private int velocidade = 5;
        private long tempoUltimoTiro;
        private List<TiroChefe> tiros;

        public Chefe(int x, int y) {
            this.x = x;
            this.y = y;
            this.tiros = new ArrayList<>();
        }

        public void atualizar(NaveJogador jogador, int velocidadeTiroChefe, long frequenciaTiroChefe) {
            mover();
            dispararTiro(jogador, velocidadeTiroChefe, frequenciaTiroChefe);
        }

        private void mover() {
            x += velocidade; // Move o chefe
            if (x + 120 >= 800 || x <= 0) {
                velocidade = -velocidade;
                y += 20; // Move para baixo a cada inversão
            }
        }

        private void dispararTiro(NaveJogador jogador, int velocidadeTiroChefe, long frequenciaTiroChefe) {
            long tempoAtual = System.currentTimeMillis();
            if (tempoAtual - tempoUltimoTiro >= frequenciaTiroChefe) {
                int tiroX = x + 60; // Tiro começa do meio do chefe
                int tiroY = y + 60; // Tiro começa do meio do chefe
                tiros.add(new TiroChefe(tiroX, tiroY, velocidadeTiroChefe)); // Tiro na posição do chefe
                tempoUltimoTiro = tempoAtual;
            }
        }

        public void draw(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, 120, 60);
            for (TiroChefe tiro : tiros) {
                tiro.draw(g);
            }
        }

        public void receberDano() {
            vida--;
        }

        public boolean isDerrotado() {
            return vida <= 0;
        }

        public List<TiroChefe> getTiros() {
            return tiros; // Retorna a lista de tiros do chefe
        }
    }
}

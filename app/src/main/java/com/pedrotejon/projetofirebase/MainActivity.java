package com.pedrotejon.projetofirebase;

import static java.util.UUID.randomUUID;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db;
    ConstraintLayout lytLogin, lytLivros, lytEdtLivro, lytLeitor;
    EditText edtNome, edtAutor, edtTotalPaginas, edtPaginasLidas, edtAvaliacao, edtBuscar;
    TextView lblFileName;
    Button btnImportar;
    ImageView pdfView, imgLeitor;
    RecyclerView recyclerLivros;
    List<Livro> listaLivros = new ArrayList<>();
    LivroAdapter adapter;
    Livro livroAtual;
    String search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        edtNome = findViewById(R.id.txtNome);
        edtAutor = findViewById(R.id.txtAutor);
        edtTotalPaginas = findViewById(R.id.txtTotalPaginas);
        edtPaginasLidas = findViewById(R.id.txtPaginasLidas);
        edtAvaliacao = findViewById(R.id.txtAvaliacao);
        edtBuscar = findViewById(R.id.txtBuscar);
        recyclerLivros = findViewById(R.id.recyclerLivros);
        recyclerLivros.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LivroAdapter(listaLivros);
        recyclerLivros.setAdapter(adapter);
        pdfView = findViewById(R.id.pdfView);
        btnImportar = findViewById(R.id.btnImportar);
        lblFileName = findViewById(R.id.lblFileName);
        lytLivros = findViewById(R.id.lytLivros);
        lytLogin = findViewById(R.id.lytLogin);
        lytEdtLivro = findViewById(R.id.lytEdtLivro);
        lytLeitor = findViewById(R.id.lytLeitor);
        imgLeitor = findViewById(R.id.imgLeitor);

        btnImportar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContent.launch("application/pdf");
            }
        });

        edtBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                search = edtBuscar.getText().toString().trim();
                buscar();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        carregarLivros();
    }

    public void voltarMenuLivros(View v) {
        lytEdtLivro.setVisibility(View.GONE);
        lytLeitor.setVisibility(View.GONE);
        lytLivros.setVisibility(View.VISIBLE);
        salvarLivro(v);
        limparCampos();
    }

    public void salvarLivro(View v) {
        String nome = edtNome.getText().toString();
        String autor = edtAutor.getText().toString();
        int paginasLidas = Integer.parseInt(edtPaginasLidas.getText().toString());
        int paginasTotais = Integer.parseInt(edtTotalPaginas.getText().toString());
        float avaliacao = Float.parseFloat(edtAvaliacao.getText().toString());

        if (livroAtual == null) {
            Livro livro = new Livro(nome, autor, paginasTotais, paginasLidas, filePath, avaliacao);
            livroAtual = livro;
            db.collection("livros")
                    .add(livro)
                    .addOnSuccessListener(doc -> {
                        livroAtual.setId(doc.getId());
                        Toast.makeText(this, "Livro salvo!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao salvar livro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            livroAtual.setTitulo(nome);
            livroAtual.setAutor(autor);
            livroAtual.setTotalPaginas(paginasTotais);
            livroAtual.setPaginasLidas(paginasLidas);
            livroAtual.setAvaliacao(avaliacao);

            db.collection("livros").document(livroAtual.getId())
                    .set(livroAtual)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Livro atualizado!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    });
        }
    }

    private void limparCampos() {
        edtNome.setText("");
        edtAutor.setText("");
        edtAvaliacao.setText("");
        edtTotalPaginas.setText("");
        edtPaginasLidas.setText("");
        lblFileName.setText("");
        ((Button) findViewById(R.id.btnSalvarLivro)).setText("Salvar Livro");
    }

    private void carregarLivros() {
        db.collection("livros")
                .get()
                .addOnSuccessListener(query -> {
                    listaLivros.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Livro p = doc.toObject(Livro.class);
                        p.setId(doc.getId());
                        listaLivros.add(p);
                    }
                    adapter.notifyDataSetChanged();
                });

        adapter.setOnItemClickListener(this::carregarMenuEdicao);

    }

    public void deletarLivro(String id) {
        db.collection("l").document(id)
                .delete()
                .addOnSuccessListener(aVoid -> carregarLivros());
    }

    public void atualizarLivro(Livro livro) {
        db.collection("livros").document(livro.getId())
                .set(livro)
                .addOnSuccessListener(aVoid -> carregarLivros());
    }

    public void cadastrarUsuario(View v)
    {
        EditText edtEmail = findViewById(R.id.txtEmail);
        EditText edtSenha = findViewById(R.id.txtSenha);
        mAuth.createUserWithEmailAndPassword(edtEmail.getText().toString(), edtSenha.getText().toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Usuário criado com sucesso", Toast.LENGTH_LONG).show();
                        Log.d("FIREBASE", "Usuário criado com sucesso");
                    } else {
                        Toast.makeText(this, "Erro ao criar usuário: " + task.getException(), Toast.LENGTH_LONG).show();
                        Log.e("FIREBASE", "Erro ao criar usuário", task.getException());
                    }
                });
    }

    public void logarUsuario(View v)
    {
        EditText edtEmail = findViewById(R.id.txtEmail);
        EditText edtSenha = findViewById(R.id.txtSenha);
        mAuth.signInWithEmailAndPassword(edtEmail.getText().toString(), edtSenha.getText().toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        findViewById(R.id.lytLogin).setVisibility(View.GONE);
                        findViewById(R.id.lytLivros).setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Login bem-sucedido", Toast.LENGTH_LONG).show();
                        Log.d("FIREBASE", "Login bem-sucedido");
                    } else {
                        Toast.makeText(this, "Erro no login: " + task.getException(), Toast.LENGTH_LONG).show();
                        Log.e("FIREBASE", "Erro no login", task.getException());
                    }
                });

    }

    public void abrirMenuAdicao(View v) {
        lytLivros.setVisibility(View.GONE);
        lytEdtLivro.setVisibility(View.VISIBLE);
        livroAtual = null;
        limparCampos();
    }

    public void buscar() {
        search = edtBuscar.getText().toString().trim();
        if (search.isEmpty()) {
            carregarLivros();
        } else {
            db.collection("livros")
                    .whereEqualTo("titulo", search)
                    .get()
                    .addOnSuccessListener(query -> {
                        listaLivros.clear();
                        for (QueryDocumentSnapshot doc : query) {
                            Livro p = doc.toObject(Livro.class);
                            p.setId(doc.getId());
                            listaLivros.add(p);
                        }
                        adapter.notifyDataSetChanged();
                    });
        }
    }

    public void abrirMenuLeitura(View v) throws IOException {
        salvarLivro(v);
        lytEdtLivro.setVisibility(View.GONE);
        lytLeitor.setVisibility(View.VISIBLE);

        File destFile = new File(getFilesDir(), livroAtual.getPath());
        var uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", destFile);
        var fileDescriptor = carregarPdf(uri);
        Bitmap bitmap = obterPaginaPdf(livroAtual.getPaginasLidas());
        imgLeitor.setImageBitmap(bitmap);
        pdfRenderer.close();
        fileDescriptor.close();
    }

    public void paginaAnterior(View v) throws IOException {
        livroAtual.setPaginasLidas(Math.max(0, livroAtual.getPaginasLidas() - 1));
        atualizarLivro(livroAtual);

        File destFile = new File(getFilesDir(), livroAtual.getPath());
        var uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", destFile);
        var fileDescriptor = carregarPdf(uri);

        Bitmap bitmap = obterPaginaPdf(livroAtual.getPaginasLidas());
        imgLeitor.setImageBitmap(bitmap);

        pdfRenderer.close();
        fileDescriptor.close();
    }

    public void proximaPagina(View v) throws IOException {
        livroAtual.setPaginasLidas(Math.min(livroAtual.getTotalPaginas(), livroAtual.getPaginasLidas() + 1));
        atualizarLivro(livroAtual);

        File destFile = new File(getFilesDir(), livroAtual.getPath());
        var uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", destFile);
        var fileDescriptor = carregarPdf(uri);

        Bitmap bitmap = obterPaginaPdf(livroAtual.getPaginasLidas());
        imgLeitor.setImageBitmap(bitmap);

        pdfRenderer.close();
        fileDescriptor.close();
    }

    public void abrirMenuEdicao(View v) throws IOException {
        lytEdtLivro.setVisibility(View.VISIBLE);
        lytLeitor.setVisibility(View.GONE);
        lytLivros.setVisibility(View.GONE);
        carregarMenuEdicao(livroAtual);
    }

    public void carregarMenuEdicao(Livro livro) throws IOException {
        edtNome.setText(livro.getTitulo());
        edtAutor.setText(livro.getAutor());
        edtTotalPaginas.setText(String.valueOf(livro.getTotalPaginas()));
        edtPaginasLidas.setText(String.valueOf(livro.getPaginasLidas()));
        edtAvaliacao.setText(String.valueOf(livro.getAvaliacao()));

        livroAtual = livro;
        ((Button) findViewById(R.id.btnSalvarLivro)).setText("Atualizar Livro");
        lytEdtLivro.setVisibility(View.VISIBLE);
        lytLeitor.setVisibility(View.GONE);
        lytLivros.setVisibility(View.GONE);

        File destFile = new File(getFilesDir(), livro.getPath());
        var uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", destFile);
        var fileDescriptor = carregarPdf(uri);
        lblFileName.setText(fileName);
        Bitmap bitmap = obterPaginaPdf(0);
        pdfView.setImageBitmap(bitmap);
        pdfRenderer.close();
        fileDescriptor.close();
    }

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    try{
                        filePath = randomUUID().toString() + ".pdf";
                        File destFile = new File(getFilesDir(), filePath);

                        try (InputStream in = getContentResolver().openInputStream(uri);
                             OutputStream out = new FileOutputStream(destFile)) {

                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = in.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                            }
                        }
                        uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", destFile);
                        var fileDescriptor = carregarPdf(uri);

                        lblFileName.setText(fileName);
                        edtPaginasLidas.setText("0");
                        edtTotalPaginas.setText(String.valueOf(pdfRenderer.getPageCount()));

                        Bitmap bitmap = obterPaginaPdf(0);
                        pdfView.setImageBitmap(bitmap);

                        pdfRenderer.close();
                        fileDescriptor.close();
                    }catch(IOException ioe){
                        ioe.printStackTrace();
                    }
                }
            });

    PdfRenderer pdfRenderer;
    String fileName = "";
    String filePath;

    public ParcelFileDescriptor carregarPdf(Uri uri) throws IOException {
        ParcelFileDescriptor fileDescriptor = getContentResolver().openFile(uri, "r", new CancellationSignal());
        fileName = "";
        var cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                fileName = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        pdfRenderer = new PdfRenderer(fileDescriptor);
        return fileDescriptor;
    }

    public Bitmap obterPaginaPdf(int page) throws IOException {
        PdfRenderer.Page rendererPage = pdfRenderer.openPage(page);
        int rendererPageWidth = rendererPage.getWidth();
        int rendererPageHeight = rendererPage.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(
                rendererPageWidth,
                rendererPageHeight,
                Bitmap.Config.ARGB_8888);
        rendererPage.render(bitmap, null, null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        rendererPage.close();
        return bitmap;
    }

}
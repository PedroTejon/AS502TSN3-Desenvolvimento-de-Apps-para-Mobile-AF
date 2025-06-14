package com.pedrotejon.projetofirebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;

public class LivroAdapter extends RecyclerView.Adapter<LivroAdapter.ViewHolder> {
    private List<Livro> livros;

    public interface OnItemClickListener {
        void onItemClick(Livro livro) throws IOException;
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public LivroAdapter(List<Livro> livros) {
        this.livros = livros;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int pos) {
        Livro p = livros.get(pos);
        holder.txt1.setText(p.getTitulo() + " - " + p.getAutor() );
        holder.txt2.setText("Progresso: " + String.valueOf(p.getPaginasLidas()) + "/" + String.valueOf(p.getTotalPaginas()) + "             Avaliação: " + String.valueOf(p.getAvaliacao()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                try {
                    listener.onItemClick(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                deletarLivro(p.getId(), holder.getAdapterPosition(), v);

                return false;
            }
        });
    }

    private void deletarLivro(String idDocumento, int position, View view) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid(); // se quiser usar

            FirebaseFirestore.getInstance().collection("livros")
                    .document(idDocumento)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        livros.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(view.getContext(), "Livro deletado!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(view.getContext(), "Erro ao deletar", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(view.getContext(), "Você precisa estar logado para realizar essa ação", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return livros.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt1, txt2;
        public ViewHolder(View itemView) {
            super(itemView);
            txt1 = itemView.findViewById(android.R.id.text1);
            txt2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
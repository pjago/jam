using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class Patterns1 : MonoBehaviour
{

    // max 3 notas ao mesmo tempo eu acho.
    // vou associar direto com os controles (keycodes)   

    public KeyCode notaMain;
    public KeyCode nota2;
    public KeyCode nota3;
    public KeyCode inicio;
    public List<KeyCode> validnotes = new List<KeyCode>();

    // Start is called before the first frame update
    void Start()
    {
        notaMain = KeyCode.W;
        KeyCode[] notas = { KeyCode.W, KeyCode.S, KeyCode.D, KeyCode.T, KeyCode.G,
            KeyCode.H, KeyCode.J, KeyCode.O, KeyCode.L};
        //outras notas não pertencem à escala! also, as notas vão sair daqui.
        validnotes.AddRange(notas);
        //ok como ver se essa droga deu certo
        //Debug.Log(validnotes.Count);
        Pattern1();

    }

    // Update is called once per frame
    void Update()
    {
       // Pattern1();
    }

    void Pattern1()
    {
        inicio = notaMain;
        int position = validnotes.IndexOf(inicio);
        int size = validnotes.Count;
        int i;
        int prob = 100;
        //com position, podemos criar o mapa de probabilidades para a proxima nota!
        Dictionary<int, int> probmap = new Dictionary<int, int>();
        for (i = position; i < size; i++) {
            probmap[i] = prob;
            prob = prob / 2;

        }
        int probsum = probmap.Sum(x => x.Value); //linq = magic

        System.Random choose = new System.Random();
        int choo = choose.Next(probsum);

        for(i = position; i < size; i++)
        {
            if (choo < probmap[i])
                nota2 = validnotes[i];

            choo = choo - probmap[i];
                
        }

        Debug.Log(probmap.Sum(x => x.Value));
        
    }

}

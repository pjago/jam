using System;
using System.Collections;
using System.Collections.Generic;
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
        var output = JsonUtility.ToJson(validnotes, true);
        Debug.Log(output);
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
        double prob = 0.5;
        //com position, podemos criar o mapa de probabilidades para a proxima nota!
        Dictionary<int, double> probmap = new Dictionary<int, double>();
        for (i = position; i < size; i++) {
            probmap[i] = prob;
            prob = prob / 2;

        }
        Debug.Log(probmap);

        probmap[position + 1] = 0.5;
        probmap[position + 2] = 0.25;
        probmap[position + 3] = 0.125;
        probmap[position + 4] = 0.0625;
        probmap[position] = 0.0625;


    }

}

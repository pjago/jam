﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SoundPiano : MonoBehaviour
{
    public AudioSource teclaP;
    public KeyCode chave;
    // Start is called before the first frame update
    void Start()
    {
        
    }

    // Update is called once per frame
    void Update()
    {
        if (Input.GetKeyDown(chave)) {
            teclaP.Play();
        }
    }
}

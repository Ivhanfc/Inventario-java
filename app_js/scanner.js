<<<<<<< HEAD
//constantes de html via id
=======

// Referencias HTML

>>>>>>> ivan
const video = document.getElementById('video');
const canvas = document.getElementById('canvas');
const resultEl = document.getElementById('result');
const startBtn = document.getElementById('startBtn');
<<<<<<< HEAD
//variables de control
let stream = null;
let animationId = null;
//funcion para mostrar el resultado y mandarlo
function showResult(id) {
    resultEl.textContent = id ?? '_';
    console.log(id);
    fetch('http://127.0.0.1:8080/api/id', {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({ id: id })
    })
      .then(res => res.text())
    .then(data => console.log("Respuesta del servidor:", data))
    .catch(err => console.error("Error al enviar:", err));
}
//importacion de tryjsQR
async function  tryJsQR(imageData) {
    if (!window.jsQR) {
        await import('https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.min.js')
    .catch(e => {console.warn('No se cargo el jsQR', e); });
    console.log("qrjd cargado")
    }
    if (!window.jsQR) return null;
    const code = window.jsQR(imageData.data, imageData.width, imageData.height);
    return code ? code.data : null;
}
//funcion para el bucle de escaneo
async function  scanLoop() {
    if (video.videoWidth === 0 || video.videoHeight === 0) {
            animationId = requestAnimationFrame(scanLoop);
        return;
    }

canvas.width = video.videoWidth;
canvas.height = video.videoHeight;
const ctx = canvas.getContext('2d');
ctx.drawImage(video, 0,0, canvas.width, canvas.height);

const imageData = ctx.getImageData(0,0,canvas.width,canvas.height);
console.log("Píxeles analizados:", imageData.data.length);
const qr = await tryJsQR(imageData);
if (qr) {
    showResult(qr);
    cancelAnimationFrame(animationId);
    return;
}
animationId = requestAnimationFrame(scanLoop);
}
//funcion para prender la camara
async function startCamera() {
try {
    if(stream) {
        video.srcObject =stream;
    }
    else {
        const constraints = {
            video: {
                facingMode: {ideal: 'environment'},
                width: {ideal: 1280},
                height: {ideal: 720}
            },
            audio: false
        };
        stream = await navigator.mediaDevices.getUserMedia(constraints);
        video.srcObject = stream;
    
}
    video.play();
    showResult('buscando...');
    animationId = requestAnimationFrame(scanLoop);
}
catch(err) {
    console.error('No se pudo acceder a la camara', err);
    showResult('Error camara: '+ (err.message || err));
}
}
    startBtn.addEventListener('click', startCamera);
 window.addEventListener('beforeunload', () => {
    if (stream) {
        stream.getTracks().forEach(t => t.stop());
    }
 });   

    

    
=======


// Variables de control

let stream = null;
let animationId = null;
let scanning = false;
let ctx = null;

const API_URL = 'http://127.0.0.1:8080/api/id';


// Mostrar el resultado y enviarlo al backend

function showResult(id) {
    resultEl.textContent = id ?? '_';
    console.log("QR detectado:", id);

    fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id })
    })
        .then(res => res.text())
        .then(data => console.log("Respuesta del servidor:", data))
        .catch(err => console.error("Error al enviar:", err));
}


// Cargar jsQR dinámicamente

function loadJsQR() {
    return new Promise((resolve, reject) => {
        if (window.jsQR) return resolve();

        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.min.js';
        script.onload = () => resolve();
        script.onerror = (e) => reject(e);

        document.head.appendChild(script);
    });
}


// Intento de lectura QR

async function tryJsQR(imageData) {
    try {
        await loadJsQR();
    } catch (e) {
        console.warn("No se pudo cargar jsQR:", e);
        return null;
    }

    if (!window.jsQR) return null;

    const code = window.jsQR(imageData.data, imageData.width, imageData.height);
    return code ? code.data : null;
}


// Bucle de escaneo

async function scanLoop() {

    // Esperar a que el video tenga size
    if (video.videoWidth === 0 || video.videoHeight === 0) {
        animationId = requestAnimationFrame(scanLoop);
        return;
    }

    // Inicializar canvas una sola vez
    if (!ctx) {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx = canvas.getContext('2d');
    }

    // Dibujar frame del video
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    // Extraer pixels
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);

    // Intentar detectar QR
    const qr = await tryJsQR(imageData);

    if (qr) {
        showResult(qr);
        stopScan();
        return;
    }

    animationId = requestAnimationFrame(scanLoop);
}


// Iniciar cámara y escaneo

async function startCamera() {
    if (scanning) return; // evitar doble inicio

    scanning = true;
    startBtn.disabled = true;

    try {
        if (!stream) {
            const constraints = {
                video: {
                    facingMode: { ideal: 'environment' },
                    width: { ideal: 1280 },
                    height: { ideal: 720 }
                },
                audio: false
            };

            stream = await navigator.mediaDevices.getUserMedia(constraints);
        }

        video.srcObject = stream;
        await video.play();

        showResult("Buscando...");
        animationId = requestAnimationFrame(scanLoop);

    } catch (err) {
        console.error("No se pudo acceder a la cámara", err);
        showResult("Error: " + (err.message || err));
        scanning = false;
        startBtn.disabled = false;
    }
}


// Detener escaneo (al encontrar QR)

function stopScan() {
    cancelAnimationFrame(animationId);
    scanning = false;
    startBtn.disabled = false;
}


// Eventos

startBtn.addEventListener('click', startCamera);

window.addEventListener('beforeunload', () => {
    if (stream) {
        stream.getTracks().forEach(t => t.stop());
    }
});
>>>>>>> ivan

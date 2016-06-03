using System;
using UnityEngine;
using System.Collections;
using System.Linq;

public class Player : MonoBehaviour
{
    [Header("Player Attributes")]
    public float MoveSpeed;
    public float JumpHeight;

    private bool jumping;
    private bool inAir;

    public bool Growing { get; private set; }
    public bool Attached { get; private set; }
    public bool Dragging { get; private set; }

    private float dragTime;

    protected Rigidbody2D rigidbody;
    public Rigidbody2D Rigidbody
    {
        get { return rigidbody ?? (rigidbody = GetComponent<Rigidbody2D>()); }
    }

    protected FixedJoint2D joint;
    public FixedJoint2D Joint
    {
        get { return joint ?? (joint = GetComponent<FixedJoint2D>()); }
    }

    protected Collider2D collider;
    public Collider2D Collider
    {
        get { return collider ?? (collider = GetComponent<Collider2D>()); }
    }

    private void Awake()
    {
        transform.position = GameManager.Instance.SpawnPoint.transform.position;
    }

    private void Update()
    {
        if (Input.GetMouseButtonDown(0) && !Input.GetMouseButton(1) && !Growing)
        {
            var clickLocation = UIManager.Instance.MouseWorldPosition;
            CreateRope(clickLocation);
        }
        else if (Input.GetMouseButtonDown(1) && !Input.GetMouseButton(0))
        {
            var raycastHits = Physics2D.RaycastAll(UIManager.Instance.MouseWorldPosition, Vector2.zero);
            Dragging = raycastHits.Select(hit => hit.transform).Contains(transform);

            if (Dragging)
                dragTime = 0;
        }

        if (Input.GetMouseButtonUp(1)) Dragging = false;

        if (Growing) return;

        if (Attached)
        {
            if (Dragging)
            {
                var dragPosition = transform.position +
                    (UIManager.Instance.MouseWorldPosition - transform.position) / 12;
                Rigidbody.MovePosition(dragPosition);

                dragTime += Time.deltaTime;
            }

            if (Input.GetAxis("Jump") > 0)
            {
                GameManager.Instance.BreakRope();
            }

            return;
        }

        var jumpState = JumpHeight * Input.GetAxis("Jump");
        if (!InputHelper.GetAxisDown("Jump") || inAir)
        {
            jumpState = 0;
        }
        else
        {
            jumping = true;
            inAir = true;
        }

        if (InputHelper.GetAxisUp("Jump"))
        {
            jumping = false;
        }

        var horizontalState = Input.GetAxis("Horizontal");

        Rigidbody.velocity = new Vector2(MoveSpeed * horizontalState,
            Rigidbody.velocity.y + jumpState);
    }

    public void CreateRope(Vector2 direction)
    {
        var rope = Instantiate(GameManager.Instance.RopePrefab).GetComponent<Rope>();

        rope.transform.position = transform.position;
        GameManager.Instance.Rope = rope;

        Growing = true;
    }

    public void AddConnection()
    {
        Growing = false;
        Attached = true;

        Joint.enabled = true;
    }

    public void BreakRope()
    {
        if (Joint != null)
        {
            Rigidbody.AddForce(Joint.reactionForce);
        }

        Attached = false;
        Dragging = false;
        Growing = false;

        Joint.enabled = false;
        Rigidbody.AddForce(GameManager.Instance.AverageReactionForce);
    }

    public void OnTriggerExit2D(Collider2D collision)
    {
        if (collision.gameObject == GameManager.Instance.KillZone.gameObject)
        {
            transform.position = GameManager.Instance.SpawnPoint.transform.position;
            Rigidbody.velocity = Vector2.zero;

            GameManager.Instance.BreakRope();
        }
    }

    public void OnCollisionEnter2D(Collision2D collision)
    {
        if (collision.contacts.Any(contactPoint2D => contactPoint2D.point.y > transform.position.y))
            return;

        inAir = false;
    }
}

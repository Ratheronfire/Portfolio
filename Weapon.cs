using System.Collections;
using UnityEngine;
using Vexe.Runtime.Types;

[DefineCategory("Weapon Properties")]

public class Weapon : DynamicActor
{
    public new Floor CurrentFloor
    {
        get
        {
            if (owner != null)
                return owner.CurrentFloor;

            return null;
        }
    }

    protected int ammoLevel;
    protected int baseAmmoCap;
    protected int baseDamage;
    protected float baseSpeed;
    protected float lastFireTime;
    protected Entity owner;
    [Category("Miscellaneous")]
    public Sprite AmmoRefillSprite;
    [Category("Miscellaneous")]
    public Sprite WeaponSprite;
    [Category("Miscellaneous")]
    public GameObject ProjectilePrefab;

    public GameObject BossProjectilePrefab;

    [Category("Weapon Properties")]
    public bool CanRapidFire;
    [Category("Weapon Properties")]
    public bool DespawnOnImpact;
    [Category("Weapon Properties")]
    public bool IsBossWeapon;
    [Category("Weapon Properties")]
    public int AmmoCapacity;
    [Category("Weapon Properties")]
    public int Damage;
    [Category("Weapon Properties")]
    public int FireRate;
    [Category("Weapon Properties")]
    public int SecondsAlive;
    [Category("Weapon Properties")]
    public float Speed;

    public Entity Owner
    {
        get { return owner; }
        set { owner = value; }
    }

    public int BaseDamage
    {
        get { return baseDamage; }
    }

    public int BaseAmmoCap
    {
        get { return baseAmmoCap; }
    }

    public float BaseSpeed
    {
        get { return baseSpeed; }
    }

    public int AmmoLevel
    {
        get { return ammoLevel; }
    }

    protected override void Start()
    {
        lastFireTime = -1;

        ammoLevel = AmmoCapacity;
        baseAmmoCap = AmmoCapacity;
        baseDamage = Damage;
        baseSpeed = Speed;

        base.Start();
    }

    protected override void Update()
    {
        if (!owner.IsFiring)
            lastFireTime = -1;

        if (ammoLevel < 0)
            ammoLevel = 0;

        base.Start();
    }

    public void ApplyUpgrade(ShopEntry se)
    {
        switch (se.upgradeType)
        {
            case UpgradeTypes.Ammo:
                AmmoCapacity += baseAmmoCap;
                ammoLevel = AmmoCapacity;
                break;

            case UpgradeTypes.Damage:
                Damage += baseDamage;
                break;

            case UpgradeTypes.Speed:
                Speed += baseSpeed;
                break;
        }
    }

    public void RefillAmmo()
    {
        ammoLevel = AmmoCapacity;
    }

    public virtual bool CanFire()
    {
        if (ammoLevel <= 0)
            return false;
        if (!CanRapidFire)
            return !owner.IsFiring;
        return Time.time - lastFireTime >= FireRate;
    }

    public virtual void FireProjectiles(Ray dir)
    {
        ammoLevel--;

        SpawnWeapon(dir);

        lastFireTime = Time.time;
    }

    protected IEnumerator SpawnThenDelay(Vector3 direction, float seconds)
    {
        yield return new WaitForSeconds(seconds);

        SpawnWeapon(new Ray(owner.transform.position, direction));
    }

    protected void SpawnWeapon(Ray ray)
    {
        var prefabToUse = ProjectilePrefab;
        if (IsBossWeapon && BossProjectilePrefab != null) prefabToUse = BossProjectilePrefab;

        var spawnedObject = Instantiate(prefabToUse, owner.transform.position, Quaternion.identity) as GameObject;

        if (GameMaster.Instance.CurrentState == GameStates.Fight)
            spawnedObject.transform.parent = GameFight.ProjectileFolder.transform;
        else
            return;

        var spawnedWeapon = spawnedObject.GetComponent<Projectile>();

        spawnedWeapon.Creator = owner;
        spawnedWeapon.WeaponParent = this;
        spawnedWeapon.transform.position = ray.origin;
        spawnedWeapon.Direction = ray.direction;
    }
}
